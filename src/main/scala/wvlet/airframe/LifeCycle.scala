package wvlet.airframe

import javax.annotation.{PostConstruct, PreDestroy}

import wvlet.log.LogSupport
import wvlet.obj.{ObjectMethod, ObjectSchema, ObjectType}

sealed trait LifeCycleHook

case class InitHook[A](obj:A, hook: A => Unit) extends LifeCycleHook {
  def run {
    hook(obj)
  }
}

case class ShutdownHook[A](obj:A, hook: A => Unit) extends LifeCycleHook {
  def run {
    hook(obj)
  }
}

class LifeCycleManager extends SessionListener with LogSupport {
  self =>

  protected var initHook    = List.empty[InitHook[_]]
  protected var shutdownHook = List.empty[ShutdownHook[_]]

  def addInitHook[A](hook:InitHook[A]) {
    synchronized {
      // Append to the front
      trace(s"Add init hook: ${hook}")
      initHook = hook :: initHook
    }
    hook.run
  }

  def addShutdownHook[A](hook:ShutdownHook[A]) {
    synchronized {
      trace(s"Add shutdown hook: ${hook}")
      shutdownHook = hook :: shutdownHook
    }
  }

  override def afterInjection(t: ObjectType, injectee: Any): Unit = {
    debug(s"Injected ${t} (${t.rawType}): $injectee")
    val schema = ObjectSchema(t.rawType)

    // Find PostConstruct annotation
    schema
    .allMethods
    .filter{_.findAnnotationOf[PostConstruct].isDefined}
    .map{x =>
      val h = InitHook(injectee, {obj:Any => x.invoke(obj.asInstanceOf[AnyRef])})
      addInitHook(h)
    }

    // Find PreDestroy annotation
    schema
    .allMethods
    .filter {_.findAnnotationOf[PreDestroy].isDefined}
    .map { x =>
      val h = ShutdownHook(injectee, {obj:Any => x.invoke(obj.asInstanceOf[AnyRef])})
      addShutdownHook(h)
    }
  }

  def start {

  }

  def shutdown {
    // TODO Configure shutdown orders using topological sort
    for(h <- shutdownHook) {
      h.run
    }
  }

}