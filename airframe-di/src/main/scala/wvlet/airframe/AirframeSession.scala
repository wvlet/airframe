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
import wvlet.airframe.lifecycle.{CloseHook, EventHookHolder, Injectee, LifeCycleManager}
import wvlet.airframe.surface.Surface
import wvlet.airframe.tracing.{DIStats, DefaultTracer, Tracer}
import wvlet.log.LogSupport

import scala.jdk.CollectionConverters._
import scala.util.Try

/**
  */
private[airframe] class AirframeSession(
    parent: Option[AirframeSession],
    sessionName: Option[String],
    val design: Design,
    stage: Stage,
    val lifeCycleManager: LifeCycleManager,
    private
    val singletonHolder: collection.mutable.Map[Surface, Any] = new ConcurrentHashMap[Surface, Any]().asScala
) extends Session
    with AirframeSessionImpl
    with LogSupport {
  self =>

  require(
    design.binding.map(_.from).distinct.length == design.binding.length,
    s"Design contains duplicate entries: [${design.binding.groupBy(_.from).map(_._2).filter(_.length > 1).mkString(", ")}]"
  )
  protected val stats: DIStats =
    parent
      .map(_.stats)
      .orElse {
        design.getStats
      }
      .getOrElse(new DIStats())

  private[airframe] val tracer: Tracer = {
    // Find a tracer from parent
    parent
      .map(_.tracer)
      .orElse(design.getTracer) // or tracer in the current design
      .getOrElse(DefaultTracer) // or the default tracer
  }

  // Build a lookup table for all bindings in the design
  private lazy val bindingTable: Map[Surface, Binding] = {
    val b = Seq.newBuilder[(Surface, Binding)]
    // Add a reference to this session to allow bind[Session]
    val sessionSurface = Surface.of[Session]
    val sessionBinding =
      ProviderBinding(
        DependencyFactory(sessionSurface, Seq.empty, LazyF0(this).asInstanceOf[Any]),
        true,
        true,
        implicitly[SourceCode]
      )
    b += sessionSurface -> sessionBinding

    // Add a reference to the design
    val designSurface = Surface.of[Design]
    val designBinding =
      ProviderBinding(
        DependencyFactory(designSurface, Seq.empty, LazyF0(this.design).asInstanceOf[Any]),
        true,
        true,
        implicitly[SourceCode]
      )
    b += designSurface -> designBinding

    // Add user-defined bindings
    design.binding.foreach(x => b += (x.from -> x))
    b.result().toMap[Surface, Binding]
  }

  private[airframe] def getSingletonOf(t: Surface): Option[Any] = {
    singletonHolder.get(t)
  }

  def sessionId: Long = hashCode()

  def name: String =
    sessionName.getOrElse {
      val current = f"session:${sessionId}%x"
      parent
        .map { p => f"${p.name} -> ${current}" }
        .getOrElse(current)
    }

  def getInstanceOf(t: Surface)(implicit sourceCode: SourceCode): AnyRef = {
    getInstance(t, t, sourceCode, this, create = false, List.empty)
  }

  override def newSharedChildSession(d: Design): Session = {
    trace(s"[${name}] Creating a new shared child session with ${d}")
    val childSession = new AirframeSession(
      parent = Some(this),
      // Should we add suffixes for child sessions?
      sessionName,
      // Inherit parent options
      new Design(design.designOptions, d.binding, d.hooks),
      stage,
      lifeCycleManager,
      singletonHolder
    )
    childSession
  }

  override def newChildSession(d: Design, inheritParentDesignOptions: Boolean): Session = {
    val childDesign = if (inheritParentDesignOptions) {
      new Design(design.designOptions, d.binding, d.hooks) // Inherit parent options
    } else {
      d
    }
    val sb =
      new SessionBuilder(
        design = childDesign,
        parent = Some(this),
        name = None,
        // Disable registration of shutdown hooks
        addShutdownHook = false,
        // Use only core lifecycle event handlers
        lifeCycleEventHandler = lifeCycleManager.coreEventHandler
      )

    val childSession = sb.create
    trace(s"[${name}] Creating a new child session ${childSession.name} with ${d}")
    childSession
  }

  // Initialize eager singleton, pre-defined instances, eager singleton providers
  private[airframe] def init: Unit = {
    debug(s"[${name}] Initializing. Stage:${stage}")
    val production = stage == Stage.PRODUCTION
    if (production) {
      debug(s"[${name}] Eagerly initializing singletons in production mode")
    }
    tracer.onSessionInitStart(this)
    design.binding.collect {
      case s @ SingletonBinding(from, to, eager, _) if production || eager =>
        getInstanceOf(from)
      case ProviderBinding(factory, provideSingleton, eager, _) if production || eager =>
        getInstanceOf(factory.from)
    }
    tracer.onSessionInitEnd(this)
    debug(s"[${name}] Completed the initialization")
  }

  def get[A](surface: Surface)(implicit sourceCode: SourceCode): A = {
    debug(s"[${name}] Get dependency [${surface}] at ${sourceCode}")
    getInstance(surface, surface, sourceCode, this, create = false, List.empty).asInstanceOf[A]
  }

  def getOrElse[A](surface: Surface, objectFactory: => A)(implicit sourceCode: SourceCode): A = {
    debug(s"[${name}] Get dependency [${surface}] (or create with factory) at ${sourceCode}")
    getInstance(surface, surface, sourceCode, this, create = false, List.empty, Some(() => objectFactory))
      .asInstanceOf[A]
  }

  private[airframe] def createNewInstanceOf[A](surface: Surface)(implicit sourceCode: SourceCode): A = {
    debug(s"[${name}] Create dependency [${surface}] at ${sourceCode}")
    getInstance(surface, surface, sourceCode, this, create = true, List.empty).asInstanceOf[A]
  }
  private[airframe] def createNewInstanceOf[A](surface: Surface, factory: => A)(implicit sourceCode: SourceCode): A = {
    debug(s"[${name}] Create dependency [${surface}] (with factory) at ${sourceCode}")
    getInstance(surface, surface, sourceCode, this, create = true, List.empty, Some(() => factory)).asInstanceOf[A]
  }

  /**
    * Called when injecting an instance of the surface for the first time. The other hooks (e.g., onStart, onShutdown)
    * will be called in a separate step after the object is injected.
    */
  private[airframe] def registerInjectee(bindTarget: Surface, tpe: Surface, injectee: Any): AnyRef = {
    debug(s"[${name}] Init [${bindTarget} -> ${tpe}]: ${injectee}")

    stats.incrementInitCount(this, tpe)
    tracer.onInitInstanceStart(this, tpe, injectee)

    observedTypes.getOrElseUpdate(tpe, System.currentTimeMillis())

    // Add additional lifecycle hooks for the injectee
    trace(s"Checking lifecycle hooks for ${tpe}: ${design.hooks.length}")
    for (hook <- findLifeCycleHooksFor(tpe)) {
      val h = EventHookHolder(tpe, injectee, hook.hook)
      lifeCycleManager.addLifeCycleHook(hook.lifeCycleHookType, h)
    }

    // Start the lifecycle of the object (injectee)
    Try(lifeCycleManager.onInit(tpe, injectee.asInstanceOf[AnyRef])).recover { case e: Throwable =>
      error(s"Error occurred while executing onInit(${tpe}, ${injectee})", e)
      throw e
    }

    /**
      * If an injected class implements close() (in AutoCloseable interface), add a shutdown hook to call close() if
      * there is no other shutdown hooks
      */
    if (classOf[AutoCloseable].isAssignableFrom(injectee.getClass)) {
      injectee match {
        case s: Session =>
        // Do not close Session automatically
        case _ =>
          if (!lifeCycleManager.hasShutdownHooksFor(bindTarget)) {
            debug(s"Add close hook for ${bindTarget}")
            lifeCycleManager.addShutdownHook(new CloseHook(new Injectee(bindTarget, injectee)))
          }
      }
    }

    tracer.onInitInstanceEnd(this, tpe, injectee)
    injectee.asInstanceOf[AnyRef]
  }

  private def findLifeCycleHooksFor(t: Surface): Seq[LifeCycleHookDesign] = {
    // trace(s"[${name}] findLifeCycleHooksFor ${t}")
    if (design.hooks.isEmpty) {
      parent.map(_.findLifeCycleHooksFor(t)).getOrElse(Seq.empty)
    } else {
      val lst = Seq.newBuilder[LifeCycleHookDesign]
      // hooks in the child session has higher precedence than that in the parent
      lst ++= design.hooks.filter(_.surface == t)
      parent.foreach { p => lst ++= p.findLifeCycleHooksFor(t) }
      lst.result()
    }
  }

  // type -> firstObservedTimeMillis
  private[airframe] val observedTypes = new ConcurrentHashMap[Surface, Long]().asScala

  /**
    * Find a session (including parent and ancestor parents) that owns t, that is, a session that can build t or has
    * ever built t.
    */
  private[airframe] def findOwnerSessionOf(t: Surface): Option[AirframeSession] = {
    if (bindingTable.contains(t) || observedTypes.contains(t)) {
      Some(this)
    } else {
      parent.flatMap(_.findOwnerSessionOf(t))
    }
  }

  private[airframe] def getInstance(
      bindTarget: Surface,
      tpe: Surface,
      sourceCode: SourceCode,
      contextSession: AirframeSession,
      create: Boolean, // true for factory binding
      seen: List[Surface],
      defaultValue: Option[() => Any] = None
  ): AnyRef = {
    stats.observe(tpe)
    tracer.onInjectStart(this, tpe)

    trace(s"[${name}] Search bindings for ${tpe}, dependencies:[${seen.mkString(" <- ")}]")
    if (seen.contains(tpe)) {
      error(s"Found cyclic dependencies: ${seen} at ${sourceCode}")
      throw new CYCLIC_DEPENDENCY(seen, sourceCode)
    }

    // Find or create an instance for the binding
    // When the instance is created for the first time, it will call onInit lifecycle hook.
    val obj =
      bindingTable.get(tpe) match {
        case None =>
          // If no binding is found in the current, traverse to the parent.
          trace(s"[${name}] Search parent for ${tpe}")
          parent.flatMap { p =>
            p.findOwnerSessionOf(tpe).map { owner =>
              // Use the parent session only when some binding is found in the parent
              owner.getInstance(bindTarget, tpe, sourceCode, contextSession, create, seen, defaultValue)
            }
          }
        case Some(b) =>
          val result =
            b match {
              case ClassBinding(from, to, sourceCode) =>
                trace(s"[${name}] Found a class binding from ${from} to ${to}, defined at ${sourceCode}")
                registerInjectee(
                  from,
                  from,
                  contextSession.getInstance(from, to, sourceCode, contextSession, create, tpe :: seen)
                )
              case sb @ SingletonBinding(from, to, eager, sourceCode) if from != to =>
                trace(s"[${name}] Found a singleton binding: ${from} => ${to}, defined at ${sourceCode}")
                singletonHolder.getOrElseUpdate(
                  from,
                  registerInjectee(
                    from,
                    from,
                    contextSession.getInstance(
                      from,
                      to,
                      sourceCode,
                      contextSession,
                      create,
                      tpe :: seen,
                      defaultValue
                    )
                  )
                )
              case sb @ SingletonBinding(from, to, eager, sourceCode) if from == to =>
                trace(s"[${name}] Found a singleton binding: ${from}, defined at ${sourceCode}")
                singletonHolder.getOrElseUpdate(
                  from,
                  registerInjectee(
                    bindTarget,
                    from,
                    contextSession.buildInstance(to, sourceCode, contextSession, seen, defaultValue)
                  )
                )
              case p @ ProviderBinding(factory, provideSingleton, eager, sourceCode) =>
                trace(s"[${name}] Found a provider for ${p.from}: ${p}, defined at ${sourceCode}")

                def buildWithProvider: Any = {
                  val dependencies = for (d <- factory.dependencyTypes) yield {
                    contextSession.getInstance(d, d, sourceCode, contextSession, false, tpe :: seen)
                  }
                  factory.create(dependencies)
                }

                if (provideSingleton) {
                  singletonHolder.getOrElseUpdate(p.from, registerInjectee(p.from, p.from, buildWithProvider))
                } else {
                  registerInjectee(p.from, p.from, buildWithProvider)
                }
              case other =>
                throw new IllegalStateException(s"Unexpected binding: ${other}")
            }
          Some(result)
      }

    val result =
      obj.getOrElse {
        // strict mode
        if (design.designOptions.defaultInstanceInjection.contains(false)) {
          throw new MISSING_DEPENDENCY(tpe :: seen, sourceCode)
        }

        trace(s"[${name}] No binding is found for ${tpe}. Building the instance. create = ${create}")
        if (create) {
          // Create a new instance for bindFactory[X] or building X using its default value
          registerInjectee(
            bindTarget,
            tpe,
            contextSession.buildInstance(tpe, sourceCode, contextSession, seen, defaultValue)
          )
        } else {
          // Create a singleton if no binding is found
          singletonHolder.getOrElseUpdate(
            tpe,
            registerInjectee(
              bindTarget,
              tpe,
              contextSession.buildInstance(tpe, sourceCode, contextSession, seen, defaultValue)
            )
          )
        }
      }

    tracer.onInjectEnd(this, tpe)
    stats.incrementInjectCount(this, tpe)

    result.asInstanceOf[AnyRef]
  }

  private[airframe] def buildInstance(
      tpe: Surface,
      sourceCode: SourceCode,
      contextSession: AirframeSession,
      seen: List[Surface],
      defaultValue: Option[() => Any]
  ): Any = {
    traitFactoryCache
      .get(tpe).map { f =>
        trace(s"[${name}] Using a pre-registered trait factory for ${tpe}")
        f(this)
      }
      .orElse {
        // Use the provided object factory if exists
        defaultValue.map { f =>
          trace(s"[${name}] Using the default value for ${tpe}")
          f()
        }
      }
      .getOrElse {
        trace(s"[${name}] No binding is found for ${tpe}")
        buildInstance(tpe, sourceCode, contextSession, tpe :: seen)
      }
  }

  /**
    * Create a new instance of the surface
    */
  private def buildInstance(
      surface: Surface,
      sourceCode: SourceCode,
      contextSession: AirframeSession,
      seen: List[Surface]
  ): Any = {
    trace(s"[${name}] buildInstance ${surface}, dependencies:[${seen.mkString(" <- ")}]")
    if (surface.isPrimitive) {
      // Cannot build Primitive types
      throw MISSING_DEPENDENCY(seen, sourceCode)
    } else {
      surface.objectFactory match {
        case Some(factory) =>
          trace(s"Using the default constructor for building ${surface} at ${sourceCode}")
          val args = for (p <- surface.params) yield {
            // When using the default constructor, we should disable singleton registration for p unless p has SingletonBinding
            // For example, when building A(p1:Long=10, p2:Long=20, ...), we should not register p1, p2 long values as singleton.
            contextSession.getInstance(
              p.surface,
              p.surface,
              sourceCode,
              contextSession,
              // If the default value for the parameter is given, do not register the instance as a singleton
              create = p.getDefaultValue.nonEmpty,
              seen,
              p.getDefaultValue.map(x => () => x)
            )
          }
          val obj = factory.newInstance(args)
          obj
        case None =>
          val obj = traitFactoryCache.get(surface) match {
            case Some(factory) =>
              trace(s"[${name}] Using pre-compiled factory for ${surface} at ${sourceCode}")
              factory.asInstanceOf[Session => Any](this)
            case None =>
              // buildWithReflection(t)
              warn(
                s"[${name}] No binding nor the default constructor for ${surface} at ${sourceCode} is found. " +
                  s"Add bind[${surface}].toXXX to your design or make sure ${surface} is not an abstract class. The dependency order: ${seen.reverse
                      .mkString(" -> ")}"
              )
              throw MISSING_DEPENDENCY(seen, sourceCode)
          }
          obj
      }
    }
  }
}
