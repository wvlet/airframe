/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe

import java.util.concurrent.ConcurrentHashMap

import com.sun.org.omg.CORBA.ContextIdSeqHelper
import wvlet.airframe.AirframeException.{CYCLIC_DEPENDENCY, MISSING_DEPENDENCY}
import wvlet.airframe.Binder._
import wvlet.log.LogSupport
import wvlet.airframe.surface.Surface

import scala.util.Try
import scala.collection.JavaConverters._

/**
  *
  */
private[airframe] class AirframeSession(parent: Option[AirframeSession],
                                        sessionName: Option[String],
                                        val design: Design,
                                        stage: Stage,
                                        val lifeCycleManager: LifeCycleManager,
                                        private val singletonHolder: collection.mutable.Map[Surface, Any] =
                                          new ConcurrentHashMap[Surface, Any]().asScala)
    extends Session
    with LogSupport {
  self =>

  require(
    design.binding.map(_.from).distinct.length == design.binding.length,
    s"Design contains duplicate entries: [${design.binding.groupBy(_.from).map(_._2).filter(_.length > 1).mkString(", ")}]"
  )

  // Build a lookup table for all bindings in the design
  private lazy val bindingTable: Map[Surface, Binding] = {
    val b = Seq.newBuilder[(Surface, Binding)]
    // Add a reference to this session to allow bind[Session]
    val sessionSurface = Surface.of[Session]
    val sessionBinding =
      ProviderBinding(DependencyFactory(sessionSurface, Seq.empty, LazyF0(this).asInstanceOf[Any]), true, true)
    b += sessionSurface -> sessionBinding

    // Add a reference to the design
    val designSurface = Surface.of[Design]
    val designBinding =
      ProviderBinding(DependencyFactory(designSurface, Seq.empty, LazyF0(this.design).asInstanceOf[Any]), true, true)
    b += designSurface -> designBinding

    // Add user-defined bindings
    design.binding.foreach(x => b += (x.from -> x))
    b.result.toMap[Surface, Binding]
  }

  private[airframe] def getSingletonOf(t: Surface): Option[Any] = {
    singletonHolder.get(t)
  }

  def name: String = sessionName.getOrElse(f"session:${hashCode()}%x")

  def getInstanceOf(t: Surface): AnyRef = {
    getInstance(t, this, create = false, List.empty)
  }

  override def newSharedChildSession(d: Design): Session = {
    trace(s"Creating a new shared child session with ${d}")
    val childSession = new AirframeSession(
      parent = Some(this),
      sessionName, // Should we add suffixes for child sessions?
      d,
      stage,
      lifeCycleManager,
      singletonHolder
    )
    childSession
  }

  override def newChildSession(d: Design): Session = {
    val l = new LifeCycleManager(lifeCycleManager.eventHandler)
    val childSession = new AirframeSession(
      parent = Some(this),
      None, // TODO Should we create a child session name?
      d,
      stage,
      l
    )
    // LifeCycleManager needs to know about the session
    l.setSession(childSession)
    trace(s"Creating a new child session ${childSession.name} with ${d}")
    childSession
  }

  // Initialize eager singleton, pre-defined instances, eager singleton providers
  private[airframe] def init: Unit = {
    debug(s"[${name}] Initializing. Stage:${stage}")
    val production = stage == Stage.PRODUCTION
    if (production) {
      debug(s"Eagerly initializing singletons in production mode")
    }
    design.binding.collect {
      case s @ SingletonBinding(from, to, eager) if production || eager =>
        getInstanceOf(from)
      case ProviderBinding(factory, provideSingleton, eager) if production || eager =>
        getInstanceOf(factory.from)
    }
    debug(s"[${name}] Completed the initialization")
  }

  private[airframe] def get[A](surface: Surface): A = {
    debug(s"Get dependency [${surface}]")
    getInstance(surface, this, create = false, List.empty).asInstanceOf[A]
  }

  private[airframe] def getOrElse[A](surface: Surface, objectFactory: => A): A = {
    debug(s"Get dependency [${surface}] or create from factory")
    getInstance(surface, this, create = false, List.empty, Some(() => objectFactory)).asInstanceOf[A]
  }

  private[airframe] def createNewInstanceOf[A](surface: Surface): A = {
    debug(s"Create a new instance of [${surface}]")
    getInstance(surface, this, create = true, List.empty).asInstanceOf[A]
  }
  private[airframe] def createNewInstanceOf[A](surface: Surface, factory: => A): A = {
    debug(s"Get dependency [${surface}] or create from factory")
    getInstance(surface, this, create = true, List.empty, Some(() => factory)).asInstanceOf[A]
  }

  /**
    * Called when injecting an instance of the surface for the first time.
    * The other hooks (e.g., onStart, onShutdown) will be called in a separate step after the object is injected.
    */
  private def registerInjectee(t: Surface, injectee: Any): AnyRef = {
    debug(s"registerInjectee[${t}], injectee:${injectee}")
    observedTypes.getOrElseUpdate(t, System.currentTimeMillis())
    Try(lifeCycleManager.onInit(t, injectee.asInstanceOf[AnyRef])).recover {
      case e: Throwable =>
        error(s"Error occurred while executing onInject(${t}, ${injectee})", e)
        throw e
    }
    injectee.asInstanceOf[AnyRef]
  }

  // type -> firstObservedTimeMillis
  private[airframe] val observedTypes = new ConcurrentHashMap[Surface, Long]().asScala

  /**
    * Find a session (including parent and ancestor parents) that owns t, that is, a session that can build t or has ever built t.
    */
  private[airframe] def findOwnerSessionOf(t: Surface): Option[AirframeSession] = {
    if (bindingTable.contains(t) || observedTypes.contains(t)) {
      Some(this)
    } else {
      parent.flatMap(_.findOwnerSessionOf(t))
    }
  }

  private[airframe] def getInstance(t: Surface,
                                    contextSession: AirframeSession,
                                    create: Boolean, // true for factory binding
                                    seen: List[Surface],
                                    defaultValue: Option[() => Any] = None): AnyRef = {
    trace(s"Search bindings for ${t}, dependencies:[${seen.mkString(" <- ")}]")
    if (seen.contains(t)) {
      error(s"Found cyclic dependencies: ${seen}")
      throw new CYCLIC_DEPENDENCY(seen.toSet)
    }

    // Find or create an instance for the binding
    // When the instance is created for the first time, it will call onInit lifecycle hook.
    val obj =
      bindingTable.get(t) match {
        case None =>
          // If no binding is found in the current, traverse to the parent
          debug(s"Search parent for ${t}")
          parent.flatMap { p =>
            p.findOwnerSessionOf(t).map { targetSession =>
              // Use the parent session only when binding is found in the parent
              targetSession.getInstance(t, contextSession, create, seen, defaultValue)
            }
          }
        case Some(b) =>
          val result =
            b match {
              case ClassBinding(from, to) =>
                trace(s"Found a class binding from ${from} to ${to}")
                registerInjectee(from, contextSession.getInstance(to, contextSession, create, t :: seen))
              case sb @ SingletonBinding(from, to, eager) if from != to =>
                trace(s"Found a singleton binding: ${from} => ${to}")
                singletonHolder.getOrElseUpdate(
                  from,
                  registerInjectee(from,
                                   contextSession.getInstance(to, contextSession, create, t :: seen, defaultValue)))
              case sb @ SingletonBinding(from, to, eager) if from == to =>
                trace(s"Found a singleton binding: ${from}")
                singletonHolder.getOrElseUpdate(
                  from,
                  registerInjectee(from, contextSession.buildInstance(to, contextSession, seen, defaultValue)))
              case p @ ProviderBinding(factory, provideSingleton, eager) =>
                trace(s"Found a provider for ${p.from}: ${p}")
                def buildWithProvider: Any = {
                  val dependencies = for (d <- factory.dependencyTypes) yield {
                    contextSession.getInstance(d, contextSession, false, t :: seen)
                  }
                  factory.create(dependencies)
                }
                if (provideSingleton) {
                  singletonHolder.getOrElseUpdate(p.from, registerInjectee(p.from, buildWithProvider))
                } else {
                  registerInjectee(p.from, buildWithProvider)
                }
            }
          Some(result)
      }

    val result =
      obj.getOrElse {
        trace(s"No binding is found for ${t}. Building the instance. create = ${create}")
        if (create) {
          // Create a new instance for bindFactory[X] or building X using its default value
          registerInjectee(t, contextSession.buildInstance(t, contextSession, seen, defaultValue))
        } else {
          // Create a singleton if no binding is found
          singletonHolder.getOrElseUpdate(
            t,
            registerInjectee(t, contextSession.buildInstance(t, contextSession, seen, defaultValue)))
        }
      }

    result.asInstanceOf[AnyRef]
  }

  private[airframe] def buildInstance(t: Surface,
                                      contextSession: AirframeSession,
                                      seen: List[Surface],
                                      defaultValue: Option[() => Any] = None): Any = {
    traitFactoryCache
      .get(t).map { f =>
        trace(s"Using a pre-registered trait factory for ${t}")
        f(this)
      }
      .orElse {
        // Use the provided object factory if exists
        defaultValue.map { f =>
          trace(s"Using the default value for ${t}")
          f()
        }
      }
      .getOrElse {
        trace(s"No binding is found for ${t}")
        buildInstance(t, contextSession, t :: seen)
      }
  }

  /**
    * Create a new instance of the surface
    */
  private def buildInstance(surface: Surface, contextSession: AirframeSession, seen: List[Surface]): Any = {
    trace(s"buildInstance ${surface}, dependencies:[${seen.mkString(" <- ")}]")
    if (surface.isPrimitive) {
      // Cannot build Primitive types
      throw MISSING_DEPENDENCY(seen)
    } else {
      surface.objectFactory match {
        case Some(factory) =>
          trace(s"Using the default constructor for building ${surface}")
          val args = for (p <- surface.params) yield {
            // When using the default constructor, we should disable singleton registration for p unless p has SingletonBinding
            // For example, when building A(p1:Long=10, p2:Long=20, ...), we should not register p1, p2 long values as singleton.
            contextSession.getInstance(p.surface,
                                       contextSession,
                                       create = true,
                                       seen,
                                       p.getDefaultValue.map(x => () => x))
          }
          val obj = factory.newInstance(args)
          obj
        case None =>
          val obj = traitFactoryCache.get(surface) match {
            case Some(factory) =>
              trace(s"Using pre-compiled factory for ${surface}")
              factory.asInstanceOf[Session => Any](this)
            case None =>
              //buildWithReflection(t)
              warn(
                s"No binding nor the default constructor for ${surface} is found. " +
                  s"Add bind[${surface}].toXXX to your design or dependencies:[${seen.mkString(" <- ")}]")
              throw MISSING_DEPENDENCY(seen)
          }
          obj
      }
    }
  }
}
