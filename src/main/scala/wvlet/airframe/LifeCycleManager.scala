package wvlet.airframe

import java.util.concurrent.atomic.AtomicReference
import javax.annotation.{PostConstruct, PreDestroy}

import wvlet.log.LogSupport
import wvlet.obj.{ObjectMethod, ObjectSchema, ObjectType}

sealed trait LifeCycleStage
case object INIT extends LifeCycleStage
case object STARTING extends LifeCycleStage
case object STARTED extends LifeCycleStage
case object STOPPING extends LifeCycleStage
case object STOPPED extends LifeCycleStage


object LifeCycleManager {
  val DEFAULT_LIFECYCLE_EVENT_HANDLER =
    ShowLifeCycleLog wraps
      (JSR330AnnotationHandler andThen FIFOHookExecutor)
}

/**
  * LifeCycleManager manages the life cycle of objects within a Session
  */
class LifeCycleManager(eventHandler:LifeCycleEventHandler) extends LogSupport {
  self =>

  private val state = new AtomicReference[LifeCycleStage](INIT)
  def currentState: LifeCycleStage = state.get()

  private[airframe] def onInit(t:ObjectType, injectee:AnyRef) {
    eventHandler.onInit(this, t, injectee)
  }

  def start {
    if (!state.compareAndSet(INIT, STARTING)) {
      throw new IllegalStateException(s"LifeCycle is already starting")
    }

    // Add shutdown hook
    sys.addShutdownHook {
      self.shutdown
    }

    eventHandler.beforeStart(this)
    // Run start hooks in the registration order
    startHook.reverse.foreach(_.execute)
    state.set(STARTED)
    eventHandler.afterStart(this)
  }

  def shutdown {
    if (state.compareAndSet(STARTED, STOPPING)) {
      eventHandler.beforeShutdown(this)
      // Run shutdown hooks in the reverse registration order
      shutdownHook.foreach(_.execute)
      state.set(STOPPED)
      eventHandler.afterShutdown(this)
    }
  }

  def addInitHook(h: LifeCycleHook) {
    trace(s"Add init hook: ${h}")
    // Immediately execute the init hook
    h.execute
  }

  private var startHook    = List.empty[LifeCycleHook]
  private var shutdownHook = List.empty[LifeCycleHook]

  def startHooks = startHook
  def shutdownHooks = shutdownHook

  def addStartHook(h: LifeCycleHook) {
    trace(s"Add start hook: ${h}")
    synchronized {
      startHook = h :: startHook
    }
  }

  def addShutdownHook(h: LifeCycleHook) {
    trace(s"Add shutdown hook: ${h}")
    synchronized {
      shutdownHook = h :: shutdownHook
    }
  }
}

trait LifeCycleHook {
  def execute: Unit
}
case class EventHookHolder[A](obj: A, hook: A => Unit) extends LifeCycleHook {
  def execute {
    hook(obj)
  }
}
case class ObjectMethodCall(obj: AnyRef, method: ObjectMethod) extends LifeCycleHook {
  def execute {
    method.invoke(obj)
  }
}

