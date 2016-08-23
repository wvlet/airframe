package wvlet.airframe

import wvlet.log.LogSupport
import wvlet.obj.ObjectType
import wvlet.obj.tag._

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}

object Binder {
  sealed trait Binding {
    def from: ObjectType
  }
  case class ClassBinding(from: ObjectType, to: ObjectType) extends Binding
  case class InstanceBinding(from: ObjectType, to: Any) extends Binding
  case class SingletonBinding(from: ObjectType, to: ObjectType, isEager: Boolean) extends Binding
  case class ProviderBinding[A](from: ObjectType, provider: ObjectType => A) extends Binding
  case class FactoryBinding[A, D1](from: ObjectType, d1:ObjectType, factory: D1 => A) extends Binding
}

import wvlet.airframe.Binder._

/**
  *
  */
class Binder[A](design: Design, from: ObjectType) extends LogSupport {

  def to[B <: A](implicit ev: ru.TypeTag[B]): Design = {
    val to = ObjectType.of(ev.tpe)
    if (from == to) {
      warn(s"Binding to the same type will be ignored: ${from.name}")
      design
    }
    else {
      design.addBinding(ClassBinding(from, to))
    }
  }

  def toProvider(provider: ObjectType => A): Design = {
    design.addBinding(ProviderBinding(from, provider))
  }

  def toProvider[D1](factory: D1 => A)(implicit ev:ru.TypeTag[D1]): Design = {
    design.addBinding(FactoryBinding(from, ObjectType.of(ev.tpe), factory))
  }

  def toSingletonOf[B <: A](implicit ev: ru.TypeTag[B]): Design = {
    val to = ObjectType.of(ev.tpe)
    if (from == to) {
      warn(s"Binding to the same type will be ignored: ${from.name}")
      design
    }
    else {
      design.addBinding(SingletonBinding(from, to, false))
    }
  }

  def toEagerSingletonOf[B <: A](implicit ev: ru.TypeTag[B]): Design = {
    val to = ObjectType.of(ev.tpe)
    if (from == to) {
      warn(s"Binding to the same type will be ignored: ${from.name}")
      design
    }
    else {
      design.addBinding(SingletonBinding(from, to, true))
    }
  }

  def toInstance(any: A): Design = {
    design.addBinding(InstanceBinding(from, any))
  }

  def toSingleton: Design = {
    design.addBinding(SingletonBinding(from, from, false))
  }

  def toEagerSingleton: Design = {
    design.addBinding(SingletonBinding(from, from, true))
  }
}

