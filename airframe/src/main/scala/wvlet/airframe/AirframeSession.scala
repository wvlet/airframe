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

import wvlet.airframe.AirframeException.{CYCLIC_DEPENDENCY, MISSING_DEPENDENCY}
import wvlet.airframe.Binder._
import wvlet.log.LogSupport
import wvlet.surface.Surface

import scala.util.Try

/**
  *
  */
private[airframe] class AirframeSession(parent: Option[AirframeSession],
                                        sessionName: Option[String],
                                        val design: Design,
                                        stage: Stage,
                                        val lifeCycleManager: LifeCycleManager)
    extends Session
    with LogSupport {
  self =>
  import scala.collection.JavaConverters._

  require(
    design.binding.map(_.from).distinct.length == design.binding.length,
    s"Design contains duplicate entries: [${design.binding.groupBy(_.from).map(_._2).filter(_.length > 1).mkString(", ")}]"
  )

  // Build a lookup table for all bindings in the design
  private lazy val bindingTable: Map[Surface, Binding] = {
    val b = Seq.newBuilder[(Surface, Binding)]
    // Add a reference to this session to allow bind[Session]
    val sessionSurface = wvlet.surface.of[Session]
    val sessionBinding =
      ProviderBinding(DependencyFactory(sessionSurface, Seq.empty, LazyF0(this).asInstanceOf[Any]), true, true)
    b += sessionSurface -> sessionBinding

    // Add a reference to the design
    val designSurface = wvlet.surface.of[Design]
    val designBinding =
      ProviderBinding(DependencyFactory(designSurface, Seq.empty, LazyF0(this.design).asInstanceOf[Any]), true, true)
    b += designSurface -> designBinding

    // Add user-defined bindings
    design.binding.foreach(x => b += (x.from -> x))
    b.result.toMap[Surface, Binding]
  }

  private[airframe] def getBindingOf(t: Surface) = bindingTable.get(t)
  private[airframe] def hasSingletonOf(t: Surface): Boolean = {
    singletonHolder.contains(t)
  }

  private lazy val singletonHolder: collection.mutable.Map[Surface, Any] = new ConcurrentHashMap[Surface, Any]().asScala

  def name: String = sessionName.getOrElse(f"session:${hashCode()}%x")

  def getInstanceOf(t: Surface): AnyRef = {
    getInstance(t, List.empty)
  }

  override def newChildSession(d: Design): Session = {
    new AirframeSession(
      parent = Some(this),
      sessionName, // Should we add suffixes for child sessions?
      d,
      stage,
      lifeCycleManager
    )
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
    getInstance(surface, List.empty).asInstanceOf[A]
  }

  private[airframe] def getOrElse[A](surface: Surface, objectFactory: => A): A = {
    debug(s"Get dependency [${surface}] or create from factory")
    getInstance(surface, List.empty, Some(() => objectFactory)).asInstanceOf[A]
  }

  /**
    * Called when injecting an instance of the surface for the first time
    */
  private def registerInjectee(t: Surface, obj: Any): AnyRef = {
    trace(s"registerInjectee[${t}], injectee:${obj}")
    Try(lifeCycleManager.onInit(t, obj.asInstanceOf[AnyRef])).recover {
      case e: Throwable =>
        error(s"Error occurred while executing onInject(${t}, ${obj})", e)
        throw e
    }
    obj.asInstanceOf[AnyRef]
  }

  private def getInstance(t: Surface, seen: List[Surface], defaultValue: Option[() => Any] = None): AnyRef = {
    trace(s"Search bindings for ${t}, dependencies:[${seen.mkString(" <- ")}]")
    if (seen.contains(t)) {
      error(s"Found cyclic dependencies: ${seen}")
      throw new CYCLIC_DEPENDENCY(seen.toSet)
    }

    // Find instance from bindings
    val obj =
      bindingTable.get(t).map {
        case ClassBinding(from, to) =>
          trace(s"Found a class binding from ${from} to ${to}")
          registerInjectee(from, getInstance(to, t :: seen))
        case sb @ SingletonBinding(from, to, eager) if from != to =>
          trace(s"Found a singleton binding: ${from} => ${to}")
          singletonHolder.getOrElseUpdate(from, registerInjectee(from, getInstance(to, t :: seen, defaultValue)))
        case sb @ SingletonBinding(from, to, eager) if from == to =>
          trace(s"Found a singleton binding: ${from}")
          singletonHolder.getOrElseUpdate(from, registerInjectee(from, buildInstance(to, seen, defaultValue)))
        case p @ ProviderBinding(factory, provideSingleton, eager) =>
          trace(s"Found a provider for ${p.from}: ${p}")
          def buildWithProvider: Any = {
            val dependencies = for (d <- factory.dependencyTypes) yield {
              getInstance(d, t :: seen)
            }
            factory.create(dependencies)
          }
          if (provideSingleton) {
            singletonHolder.getOrElseUpdate(p.from, registerInjectee(p.from, buildWithProvider))
          } else {
            registerInjectee(p.from, buildWithProvider)
          }
      }

    val result =
      obj.getOrElse(buildInstance(t, seen, defaultValue))

    result.asInstanceOf[AnyRef]
  }

  private def buildInstance(t: Surface, seen: List[Surface], defaultValue: Option[() => Any] = None): Any = {
    factoryCache
      .get(t).map { f =>
        trace(s"Using a pre-registered factory for ${t}")
        f(this)
      }
      .orElse {
        // Use the provided object factory if exists
        defaultValue.map { f =>
          trace(s"Using a pre-generated instance of ${t}")
          f()
        }
      }
      .getOrElse {
        parent
          .map { p =>
            // Check the parent session
            warn(s"checking the parent for ${t}")
            p.getInstance(t, seen, defaultValue)
          }
          .getOrElse {
            trace(s"No binding is found for ${t}")
            buildInstance(t, t :: seen)
          }
      }
  }

  /**
    * Create a new instance of the surface
    */
  private def buildInstance(surface: Surface, seen: List[Surface]): Any = {
    trace(s"buildInstance ${surface}, dependencies:[${seen.mkString(" <- ")}]")
    if (surface.isPrimitive) {
      // Cannot build Primitive types
      throw MISSING_DEPENDENCY(seen)
    } else {
      surface.objectFactory match {
        case Some(factory) =>
          trace(s"Using the default constructor for injecting ${surface}")
          val args = for (p <- surface.params) yield {
            getInstance(p.surface, seen, p.getDefaultValue.map(x => () => x))
          }
          val obj = factory.newInstance(args)
          obj
        case None =>
          val obj = factoryCache.get(surface) match {
            case Some(factory) =>
              trace(s"Using pre-compiled factory for ${surface}")
              factory.asInstanceOf[Session => Any](this)
            case None =>
              //buildWithReflection(t)
              warn(
                s"No binding nor the default constructor for ${surface} is found. " +
                  s"Add bind[${surface}].toXXX to your design. dependencies:[${seen.mkString(" <- ")}]")
              throw MISSING_DEPENDENCY(seen)
          }
          obj
      }
    }
  }
}
