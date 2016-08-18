package wvlet.airframe

import javax.annotation.{PostConstruct, PreDestroy}

import wvlet.log.LogSupport
import wvlet.obj.{ObjectMethod, ObjectSchema, ObjectType}

sealed trait LifeCycleEvent
case class PostConstructEvent(obj: Any, postConstruct: ObjectMethod) extends LifeCycleEvent {
  override def toString = s"@PostConstruct ${postConstruct}"
}
case class PreDestroyEvent(obj: Any, preDestory: ObjectMethod) extends LifeCycleEvent {
  override def toString = s"@PreDestroy ${preDestory}"
}

class LifeCycleFinder extends SessionListener with LogSupport {
  self =>

  private var startList   = List.empty[PostConstructEvent]
  private var destroyList = List.empty[PreDestroyEvent]

  override def afterInjection(t: ObjectType, injectee: Any): Unit = {
    debug(s"Injected ${t} (${t.rawType}): $injectee")
    val schema = ObjectSchema(t.rawType)

    // Find PostConstruct annotation
    schema
    .allMethods
    .filter {_.findAnnotationOf[PostConstruct].isDefined}
    .map(x => PostConstructEvent(injectee, x))
    .map { e =>
      debug(s"Registered PostConstruct hook: ${e}")
      synchronized {
        // Append to the front
        startList = e :: startList
      }
    }

    // Find PreDestroy annotation
    schema
    .allMethods
    .filter {_.findAnnotationOf[PreDestroy].isDefined}
    .map(x => PreDestroyEvent(injectee, x))
    .map { e =>
      debug(s"Registered PreDestroy hook: ${e}")
      synchronized {
        // Append to the front
        destroyList = e :: destroyList
      }
    }
  }
}