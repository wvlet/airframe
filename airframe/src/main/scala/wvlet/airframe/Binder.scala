/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe

import java.util.UUID

import wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY
import wvlet.airframe.AirframeMacros._
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.language.experimental.macros

object Binder {

  sealed trait Binding extends Serializable {
    def forSingleton: Boolean = false
    def from: Surface
    def sourceCode: SourceCode
  }
  case class ClassBinding(from: Surface, to: Surface, sourceCode: SourceCode) extends Binding {
    if (from == to) {
      throw new CYCLIC_DEPENDENCY(List(to), sourceCode)
    }
  }
  case class SingletonBinding(from: Surface, to: Surface, isEager: Boolean, sourceCode: SourceCode) extends Binding {
    override def forSingleton: Boolean = true
  }
  case class ProviderBinding(
      factory: DependencyFactory,
      provideSingleton: Boolean,
      eager: Boolean,
      sourceCode: SourceCode
  ) extends Binding {
    assert(!eager || (eager && provideSingleton))
    def from: Surface                  = factory.from
    override def forSingleton: Boolean = provideSingleton

    private val uuid: UUID = UUID.randomUUID()

    override def hashCode(): Int = { uuid.hashCode() }
    override def equals(other: Any): Boolean = {
      other match {
        case that: ProviderBinding =>
          // Scala 2.12 generates Lambda for Function0, and the class might be generated every time, so
          // comparing functionClasses doesn't work
          (that canEqual this) && this.uuid == that.uuid
        case _ => false
      }
    }
  }

  case class DependencyFactory(from: Surface, dependencyTypes: Seq[Surface], factory: Any) {

    override def toString: String = {
      val deps = if (dependencyTypes.isEmpty) {
        "()"
      } else {
        s"(${dependencyTypes.mkString(",")})"
      }
      s"${deps}=>${from} [${factory}]"
    }

    def create(args: Seq[Any]): Any = {
      require(args.length == dependencyTypes.length)
      args.length match {
        case 0 =>
          // We need to copy the F0 instance in order to make Design immutable
          factory.asInstanceOf[LazyF0[_]].copy.eval
        case 1 =>
          factory.asInstanceOf[Any => Any](args(0))
        case 2 =>
          factory.asInstanceOf[(Any, Any) => Any](args(0), args(1))
        case 3 =>
          factory.asInstanceOf[(Any, Any, Any) => Any](args(0), args(1), args(2))
        case 4 =>
          factory.asInstanceOf[(Any, Any, Any, Any) => Any](args(0), args(1), args(2), args(3))
        case 5 =>
          factory.asInstanceOf[(Any, Any, Any, Any, Any) => Any](args(0), args(1), args(2), args(3), args(4))
        case other =>
          throw new IllegalStateException("Should never reach")
      }
    }
  }
}

import wvlet.airframe.Binder._

/**
  *
  */
class Binder[A](val design: Design, val from: Surface, val sourceCode: SourceCode) extends LogSupport {

  /**
    * Bind a singleton instance of B to A
    *
    * @tparam B
    */
  def to[B <: A]: Design = macro binderToSingletonOfImpl[B]

  /**
    * Bind an instance of B to A
    *
    * @tparam B
    * @return
    */
  def toInstanceOf[B <: A]: Design = macro binderToImpl[B]

  /**
    * Bind the type to a given instance. The instance will be instantiated as an eager singleton when creating a session.
    * Note that as you create a new session, new instance will be generated.
    *
    * @param any
    * @return
    */
  def toInstance(any: => A): Design = {
    trace(s"binder toInstance: ${from}")
    design.addBinding(
      ProviderBinding(DependencyFactory(from, Seq.empty, LazyF0(any).asInstanceOf[Any]), true, true, sourceCode)
    )
  }

  /**
    * Bind an instance lazily (no singleton). This is used internally for implementing bindFactory[I1 => A]
    *
    * @param any
    * @return
    */
  def toLazyInstance(any: => A): Design = {
    trace(s"binder toLazyInstance: ${from}")
    design.addBinding(
      ProviderBinding(DependencyFactory(from, Seq.empty, LazyF0(any).asInstanceOf[Any]), false, false, sourceCode)
    )
  }

  def toSingletonOf[B <: A]: Design = macro binderToSingletonOfImpl[B]

  def toEagerSingletonOf[B <: A]: Design = macro binderToEagerSingletonOfImpl[B]

  def toSingleton: Design = {
    design.addBinding(SingletonBinding(from, from, false, sourceCode))
  }

  def toEagerSingleton: Design = {
    design.addBinding(SingletonBinding(from, from, true, sourceCode))
  }

  def toInstanceProvider[D1](factory: D1 => A): Design = macro bindToProvider1[D1]
  def toInstanceProvider[D1, D2](factory: (D1, D2) => A): Design = macro bindToProvider2[D1, D2]
  def toInstanceProvider[D1, D2, D3](factory: (D1, D2, D3) => A): Design = macro bindToProvider3[D1, D2, D3]
  def toInstanceProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): Design = macro bindToProvider4[D1, D2, D3, D4]
  def toInstanceProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): Design =
    macro bindToProvider5[D1, D2, D3, D4, D5]

  def toProvider[D1](factory: D1 => A): Design = macro bindToSingletonProvider1[D1]
  def toProvider[D1, D2](factory: (D1, D2) => A): Design = macro bindToSingletonProvider2[D1, D2]
  def toProvider[D1, D2, D3](factory: (D1, D2, D3) => A): Design = macro bindToSingletonProvider3[D1, D2, D3]
  def toProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): Design =
    macro bindToSingletonProvider4[D1, D2, D3, D4]
  def toProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): Design =
    macro bindToSingletonProvider5[D1, D2, D3, D4, D5]

  def toSingletonProvider[D1](factory: D1 => A): Design = macro bindToSingletonProvider1[D1]
  def toSingletonProvider[D1, D2](factory: (D1, D2) => A): Design = macro bindToSingletonProvider2[D1, D2]
  def toSingletonProvider[D1, D2, D3](factory: (D1, D2, D3) => A): Design = macro bindToSingletonProvider3[D1, D2, D3]
  def toSingletonProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): Design =
    macro bindToSingletonProvider4[D1, D2, D3, D4]
  def toSingletonProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): Design =
    macro bindToSingletonProvider5[D1, D2, D3, D4, D5]

  def toEagerSingletonProvider[D1](factory: D1 => A): Design = macro bindToEagerSingletonProvider1[D1]
  def toEagerSingletonProvider[D1, D2](factory: (D1, D2) => A): Design = macro bindToEagerSingletonProvider2[D1, D2]
  def toEagerSingletonProvider[D1, D2, D3](factory: (D1, D2, D3) => A): Design =
    macro bindToEagerSingletonProvider3[D1, D2, D3]
  def toEagerSingletonProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): Design =
    macro bindToEagerSingletonProvider4[D1, D2, D3, D4]
  def toEagerSingletonProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): Design =
    macro bindToEagerSingletonProvider5[D1, D2, D3, D4, D5]
}
