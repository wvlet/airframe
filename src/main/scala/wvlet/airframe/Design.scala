package wvlet.airframe

import wvlet.airframe.Binder.Binding
import wvlet.log.LogSupport
import wvlet.obj.ObjectType

import scala.language.experimental.macros
import scala.reflect.runtime.{universe => ru}

/**
  * Immutable airframe design
  */
class Design(val binding: Seq[Binding], val listener: Seq[SessionListener]) extends LogSupport {

  def +(other:Design) : Design = {
    new Design(binding ++ other.binding, listener ++ other.listener)
  }

  def bind[A](implicit a: ru.TypeTag[A]): Binder[A] = {
    bind(ObjectType.of(a.tpe)).asInstanceOf[Binder[A]]
  }

  private def bind(t: ObjectType): Binder[_] = {
    trace(s"Bind ${t.name} [${t.rawType}]")
    val b = new Binder(this, t)
    b
  }

  def addListner[A](l: SessionListener): Design = {
    new Design(binding, listener :+ l)
  }

  def addBinding(b: Binding): Design = {
    trace(s"Add binding: $b")
    new Design(binding :+ b, listener)
  }

  def build[A:ru.WeakTypeTag]: A = macro AirframeMacros.buildFromDesignImpl[A]

  def newSession: Session = {
    // Override preceding bindings
    val effectiveBindings = for ((key, lst) <- binding.groupBy(_.from)) yield {
      lst.last
    }
    val keyIndex: Map[ObjectType, Int] = binding.map(_.from).zipWithIndex.map(x => x._1 -> x._2).toMap
    val sortedBindings = effectiveBindings.toSeq.sortBy(x => keyIndex(x.from))
    new SessionImpl(sortedBindings, listener)
  }

}
