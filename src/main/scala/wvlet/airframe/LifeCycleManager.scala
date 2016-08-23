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

/**
  * LifeCycleManager manages the life cycle of objects within a Session
  */
trait LifeCycleManager extends LifeCycleEventHandler with LogSupport {
  self =>

  private val state = new AtomicReference[LifeCycleStage](INIT)
  def currentState: LifeCycleStage = state.get()

  def onInit(t: ObjectType, injectee: AnyRef): Unit = {
    debug(s"Injected ${t} (class: ${t.rawType}): $injectee")
    val schema = ObjectSchema(t.rawType)

    // Find JSR330 PostConstruct annotation
    schema
    .allMethods
    .filter{_.findAnnotationOf[PostConstruct].isDefined}
    .map{x =>
      addInitHook(ObjectMethodCall(injectee, x))
    }

    // Find JSR330 PreDestroy annotation
    schema
    .allMethods
    .filter {_.findAnnotationOf[PreDestroy].isDefined}
    .map { x =>
      addShutdownHook(ObjectMethodCall(injectee, x))
    }
  }

  def start {
    if (!state.compareAndSet(INIT, STARTING)) {
      throw new IllegalStateException(s"LifeCycle is already starting")
    }

    // Add shutdown hook
    sys.addShutdownHook {
      self.shutdown
    }

    beforeStart
    // Run start hooks in the registration order
    startHook.reverse.foreach(_.execute)
    state.set(STARTED)
    afterStart
  }

  def shutdown {
    if (state.compareAndSet(STARTED, STOPPING)) {
      beforeShutdown
      // Run shutdown hooks in the reverse registration order
      shutdownHook.foreach(_.execute)
      state.set(STOPPED)
      afterShutdown
    }
  }

  def addInitHook(h: LifeCycleHook) {
    trace(s"Add init hook: ${h}")
    // Immediately execute the init hook
    h.execute
  }

  private var startHook    = List.empty[LifeCycleHook]
  private var shutdownHook = List.empty[LifeCycleHook]

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

trait LifeCycleEventListener {
  def onInit(t: ObjectType, injectee: AnyRef)
}

trait LifeCycleEventHandler extends LifeCycleEventListener {
  protected def beforeStart
  protected def afterStart
  protected def beforeShutdown
  protected def afterShutdown
}

trait DefaultLifeCycleEventHandler extends LifeCycleEventHandler {
  self: LifeCycleManager =>

  protected def beforeStart() {
    info(s"Life cycle is starting ...")
  }

  protected def afterStart() {
    info(s"======= STARTED =======")
  }

  protected def beforeShutdown() {
    info(s"Stopping life cycle ...")
  }

  protected def afterShutdown() {
    info(s"Life cycle has stopped.")
  }
}

