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
package wvlet.airframe.di

import wvlet.airframe.di.DIException.CYCLIC_DEPENDENCY
import wvlet.airframe.di.lifecycle.{AFTER_START, BEFORE_SHUTDOWN, ON_INIT, ON_INJECT, ON_SHUTDOWN, ON_START}
import wvlet.airframe.surface.Surface
import wvlet.airframe.ulid.ULID

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

    private val ulid: ULID = ULID.newULID

    override def hashCode(): Int = { ulid.hashCode() }
    override def equals(other: Any): Boolean = {
      other match {
        case that: ProviderBinding =>
          // Scala 2.12 generates Lambda for Function0, which generates the class every time.
          // So instead of comparing functionClasses, compare the ULID.
          (that canEqual this) && this.ulid == that.ulid
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

import wvlet.airframe.di.Binder._

/**
  */
class Binder[A](val design: Design, val from: Surface, val sourceCode: SourceCode) extends BinderImpl[A] {

  /**
    * Bind the type to a given instance. The instance will be instantiated as an eager singleton when creating a session.
    * Note that as you create a new session, new instance will be generated.
    *
    * @param any
    * @return
    */
  def toInstance(any: => A): DesignWithContext[A] = {
    trace(s"binder toInstance: ${from}")
    val binding =
      ProviderBinding(DependencyFactory(from, Seq.empty, LazyF0(any).asInstanceOf[Any]), true, true, sourceCode)
    design.addBinding[A](binding)
  }

  def toSingleton: DesignWithContext[A] = {
    design.addBinding[A](SingletonBinding(from, from, false, sourceCode))
  }

  def toEagerSingleton: DesignWithContext[A] = {
    design.addBinding[A](SingletonBinding(from, from, true, sourceCode))
  }

  /**
    * Called when the object is initialized for the first time
    */
  def onInit(body: A => Unit): DesignWithContext[A] = {
    design.withLifeCycleHook[A](LifeCycleHookDesign(ON_INIT, from, body.asInstanceOf[Any => Unit]))
  }

  /**
    * Called when the object is injected
    */
  def onInject(body: A => Unit): DesignWithContext[A] = {
    design.withLifeCycleHook[A](LifeCycleHookDesign(ON_INJECT, from, body.asInstanceOf[Any => Unit]))
  }

  /**
    * Called when a session using A has started
    */
  def onStart(body: A => Unit): DesignWithContext[A] = {
    design.withLifeCycleHook[A](LifeCycleHookDesign(ON_START, from, body.asInstanceOf[Any => Unit]))
  }

  /**
    * Called right after the session start process has completed
    */
  def afterStart(body: A => Unit): DesignWithContext[A] = {
    design.withLifeCycleHook[A](LifeCycleHookDesign(AFTER_START, from, body.asInstanceOf[Any => Unit]))
  }

  /**
    * Called right before the session will shutdown
    */
  def beforeShutdown(body: A => Unit): DesignWithContext[A] = {
    design.withLifeCycleHook[A](LifeCycleHookDesign(BEFORE_SHUTDOWN, from, body.asInstanceOf[Any => Unit]))
  }

  /**
    * Called when the session shutdown process has started
    */
  def onShutdown(body: A => Unit): DesignWithContext[A] = {
    design.withLifeCycleHook[A](LifeCycleHookDesign(ON_SHUTDOWN, from, body.asInstanceOf[Any => Unit]))
  }
}
