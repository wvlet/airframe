package wvlet.airframe

import wvlet.airframe.Inject.{ClassBinding, InstanceBinding, ProviderBinding, SingletonBinding}
import wvlet.log.LogSupport
import wvlet.obj.ObjectType

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}

/**
  *
  */
class Bind(h: Inject, from: ObjectType) extends LogSupport {

  def to[B](implicit ev: ru.TypeTag[B]) {
    val to = ObjectType.of(ev.tpe)
    if (from == to) {
      warn(s"Binding to the same type will be ignored: ${from.name}")
    }
    else {
      h.addBinding(ClassBinding(from, to))
    }
  }

  def toProvider[A: ClassTag](provider: ObjectType => A) {
    h.addBinding(ProviderBinding(from, provider))
  }

  def toSingletonOf[B](implicit ev: ru.TypeTag[B]) {
    val to = ObjectType.of(ev.tpe)
    if (from == to) {
      warn(s"Binding to the same type will be ignored: ${from.name}")
    }
    else {
      h.addBinding(SingletonBinding(from, to, false))
    }
  }

  def toEagerSingletonOf[B](implicit ev: ru.TypeTag[B]) {
    val to = ObjectType.of(ev.tpe)
    if (from == to) {
      warn(s"Binding to the same type will be ignored: ${from.name}")
    }
    else {
      h.addBinding(SingletonBinding(from, to, true))
    }
  }

  def toInstance(any: Any) {
    h.addBinding(InstanceBinding(from, any))
  }

  def toSingleton {
    h.addBinding(SingletonBinding(from, from, false))
  }

  def toEagerSingleton {
    h.addBinding(SingletonBinding(from, from, true))
  }
}
