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

import wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY
import wvlet.log.LogSupport
import wvlet.obj.ObjectType

import scala.reflect.runtime.{universe => ru}

object Binder {
  sealed trait Binding {
    def from: ObjectType
  }
  case class ClassBinding(from: ObjectType, to: ObjectType) extends Binding
  case class InstanceBinding(from: ObjectType, to: Any) extends Binding
  case class SingletonBinding(from: ObjectType, to: ObjectType, isEager: Boolean) extends Binding
  case class ProviderBinding(factory: DependencyFactory, provideSingleton: Boolean)
    extends Binding {
    def from: ObjectType = factory.from
  }

  trait DependencyFactory {
    def from: ObjectType
    def dependencyTypes: Seq[ObjectType]
    def create(args: Seq[Any]): Any
  }
  case class DependencyFactory1[A, D1](from: ObjectType, d1: ObjectType, factory: D1 => A)
    extends DependencyFactory {
    override def dependencyTypes: Seq[ObjectType] = Seq(d1)
    def create(args: Seq[Any]): Any = {
      require(args.length == 1)
      factory.asInstanceOf[Any => Any](args(0))
    }
  }
  case class DependencyFactory2[A, D1, D2](from: ObjectType,
                                           d1: ObjectType,
                                           d2: ObjectType,
                                           factory: (D1, D2) => A)
    extends DependencyFactory {
    override def dependencyTypes: Seq[ObjectType] = Seq(d1, d2)
    def create(args: Seq[Any]): Any = {
      require(args.length == 2)
      factory.asInstanceOf[(Any, Any) => Any](args(0), args(1))
    }
  }
}

import wvlet.airframe.Binder._

/**
  *
  */
class Binder[A](design: Design, from: ObjectType) extends LogSupport {

  def to[B <: A : ru.TypeTag]: Design = {
    val to = ObjectType.of(implicitly[ru.TypeTag[B]].tpe)
    if (from == to) {
      warn(s"Binding to the same type is not allowed: ${from.name}")
      throw new CYCLIC_DEPENDENCY(Set(to))
    }
    else {
      design.addBinding(ClassBinding(from, to))
    }
  }

  def toInstance(any: A): Design = {
    design.addBinding(InstanceBinding(from, any))
  }

  def toSingletonOf[B <: A : ru.TypeTag]: Design = {
    val to = ObjectType.of(implicitly[ru.TypeTag[B]].tpe)
    if (from == to) {
      warn(s"Binding to the same type is not allowed: ${from.name}")
      throw new CYCLIC_DEPENDENCY(Set(to))
    }
    else {
      design.addBinding(SingletonBinding(from, to, false))
    }
  }

  def toEagerSingletonOf[B <: A : ru.TypeTag]: Design = {
    val to = ObjectType.of(implicitly[ru.TypeTag[B]].tpe)
    if (from == to) {
      warn(s"Binding to the same type is not allowed: ${from.name}")
      throw new CYCLIC_DEPENDENCY(Set(to))
    }
    else {
      design.addBinding(SingletonBinding(from, to, true))
    }
  }

  def toSingleton: Design = {
    design.addBinding(SingletonBinding(from, from, false))
  }

  def toEagerSingleton: Design = {
    design.addBinding(SingletonBinding(from, from, true))
  }

  def toProvider[D1: ru.TypeTag](factory: D1 => A): Design = {
    toProviderD1(factory, false)
  }
  def toSingletonProvider[D1: ru.TypeTag](factory: D1 => A): Design = {
    toProviderD1(factory, true)
  }
  def toProvider[D1: ru.TypeTag, D2: ru.TypeTag](factory: (D1, D2) => A): Design = {
    toProviderD2(factory, false)
  }
  def toSingletonProvider[D1: ru.TypeTag, D2: ru.TypeTag](factory: (D1, D2) => A): Design = {
    toProviderD2(factory, true)
  }

  private def toProviderD1[D1: ru.TypeTag](factory: D1 => A, singleton: Boolean): Design = {
    design.addBinding(ProviderBinding(
      DependencyFactory1(
        from,
        ObjectType.of(implicitly[ru.TypeTag[D1]].tpe),
        factory),
      singleton
    ))
  }

  private def toProviderD2[D1: ru.TypeTag, D2: ru.TypeTag]
  (factory: (D1, D2) => A, singleton: Boolean): Design = {
    design.addBinding(ProviderBinding(
      DependencyFactory2(
        from,
        ObjectType.of(implicitly[ru.TypeTag[D1]].tpe),
        ObjectType.of(implicitly[ru.TypeTag[D2]].tpe),
        factory
      ),
      singleton
    ))
  }
}

