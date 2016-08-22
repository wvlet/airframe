package wvlet.airframe

import wvlet.airframe.Binder.Binding
import wvlet.log.LogSupport
import wvlet.obj.ObjectType

import scala.language.experimental.macros
import scala.reflect.runtime.{universe => ru}

/**
  * Immutable airframe design
  */
class Design(val binding: Seq[Binding]) extends LogSupport {

  def +(other: Design): Design = {
    new Design(binding ++ other.binding)
  }

  def bind[A](implicit a: ru.TypeTag[A]): Binder[A] = {
    bind(ObjectType.of(a.tpe)).asInstanceOf[Binder[A]]
  }

  private def bind(t: ObjectType): Binder[_] = {
    trace(s"Bind ${t.name} [${t.rawType}]")
    val b = new Binder(this, t)
    b
  }

  private[airframe] def addBinding(b: Binding): Design = {
    trace(s"Add binding: $b")
    new Design(binding :+ b)
  }

  def build[A: ru.WeakTypeTag]: A = macro AirframeMacros.buildFromDesignImpl[A]

  def session: SessionBuilder = {
    new SessionBuilder(this)
  }

  def newSession : Session = {
    new SessionBuilder(this).create
  }
}
