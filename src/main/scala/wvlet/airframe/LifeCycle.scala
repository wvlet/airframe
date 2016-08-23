package wvlet.airframe

import javax.annotation.{PostConstruct, PreDestroy}

import wvlet.log.LogSupport
import wvlet.obj.{ObjectMethod, ObjectSchema, ObjectType}

class DefaultlLifeCycleManager
  extends LifeCycleManager
    with DefaultLifeCycleEventHandler
    with LogSupport { self =>

}