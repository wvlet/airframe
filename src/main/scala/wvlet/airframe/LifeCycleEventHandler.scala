package wvlet.airframe

import javax.annotation.{PostConstruct, PreDestroy}

import wvlet.log.{LogSupport, Logger}
import wvlet.obj.{ObjectSchema, ObjectType}

/**
  *
  */
trait LifeCycleEventHandler {
  def onInit(lifeCycleManager:LifeCycleManager, t: ObjectType, injectee: AnyRef) {}
  def beforeStart(lifeCycleManager:LifeCycleManager) {}
  def afterStart(lifeCycleManager:LifeCycleManager) {}
  def beforeShutdown(lifeCycleManager:LifeCycleManager) {}
  def afterShutdown(lifeCycleManager:LifeCycleManager) {}

  def andThen(next : LifeCycleEventHandler) = new LifeCycleEventHandlerChain(this, next)
  def wraps(child : LifeCycleEventHandler) = new LifeCycleEventHandlerPair(this, child)
}

class LifeCycleEventHandlerChain(prev:LifeCycleEventHandler, next:LifeCycleEventHandler) extends LifeCycleEventHandler {
  override def onInit(lifeCycleManager:LifeCycleManager,t: ObjectType, injectee: AnyRef) = {
    prev.onInit(lifeCycleManager, t, injectee)
    next.onInit(lifeCycleManager, t, injectee)
  }
  override def beforeStart(lifeCycleManager:LifeCycleManager) = {
    prev.beforeStart(lifeCycleManager)
    next.beforeStart(lifeCycleManager)
  }
  override def afterStart(lifeCycleManager:LifeCycleManager) = {
    prev.afterStart(lifeCycleManager)
    next.afterStart(lifeCycleManager)
  }
  override def beforeShutdown(lifeCycleManager:LifeCycleManager) = {
    prev.beforeShutdown(lifeCycleManager)
    next.beforeShutdown(lifeCycleManager)
  }
  override def afterShutdown(lifeCycleManager:LifeCycleManager) = {
    prev.afterShutdown(lifeCycleManager)
    next.afterShutdown(lifeCycleManager)
  }
}

class LifeCycleEventHandlerPair(parent:LifeCycleEventHandler, child:LifeCycleEventHandler) extends LifeCycleEventHandler {
  override def onInit(lifeCycleManager:LifeCycleManager,t: ObjectType, injectee: AnyRef) = {
    parent.onInit(lifeCycleManager, t, injectee)
    child.onInit(lifeCycleManager, t, injectee)
  }
  override def beforeStart(lifeCycleManager:LifeCycleManager) = {
    parent.beforeStart(lifeCycleManager)
    child.beforeStart(lifeCycleManager)
  }
  override def afterStart(lifeCycleManager:LifeCycleManager) = {
    child.afterStart(lifeCycleManager)
    parent.afterStart(lifeCycleManager)
  }
  override def beforeShutdown(lifeCycleManager:LifeCycleManager) = {
    parent.beforeShutdown(lifeCycleManager)
    child.beforeShutdown(lifeCycleManager)
  }
  override def afterShutdown(lifeCycleManager:LifeCycleManager) = {
    child.afterShutdown(lifeCycleManager)
    parent.afterShutdown(lifeCycleManager)
  }
}

object ShowLifeCycleLog extends LifeCycleEventHandler {

  private val logger = Logger.of[LifeCycleManager]

  override def beforeStart(lifeCycleManager:LifeCycleManager) {
    logger.info(s"Life cycle is starting ...")
  }

  override def afterStart(lifeCycleManager:LifeCycleManager) {
    logger.info(s"======= STARTED =======")
  }

  override def beforeShutdown(lifeCycleManager:LifeCycleManager) {
    logger.info(s"Stopping life cycle ...")
  }

  override def afterShutdown(lifeCycleManager:LifeCycleManager) {
    logger.info(s"Life cycle has stopped.")
  }
}


object JSR330AnnotationHandler extends LifeCycleEventHandler with LogSupport {
  override def onInit(lifeCycleManager:LifeCycleManager, t: ObjectType, injectee: AnyRef) {
    debug(s"Injected ${t} (class: ${t.rawType}): $injectee")
    val schema = ObjectSchema(t.rawType)

    // Find JSR330 @PostConstruct annotation
    schema
    .allMethods
    .filter{_.findAnnotationOf[PostConstruct].isDefined}
    .map{x =>
      lifeCycleManager.addInitHook(ObjectMethodCall(injectee, x))
    }

    // Find JSR330 @PreDestroy annotation
    schema
    .allMethods
    .filter {_.findAnnotationOf[PreDestroy].isDefined}
    .map { x =>
      lifeCycleManager.addShutdownHook(ObjectMethodCall(injectee, x))
    }
  }
}

object FIFOHookExecutor extends LifeCycleEventHandler {
  override def beforeStart(lifeCycleManager: LifeCycleManager): Unit = {
    lifeCycleManager.startHooks.reverse.map(_.execute)
  }

  override def beforeShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    lifeCycleManager.shutdownHooks.map(_.execute)
  }
}