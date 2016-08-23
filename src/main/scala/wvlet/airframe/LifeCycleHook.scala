package wvlet.airframe

import wvlet.obj.ObjectMethod


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

