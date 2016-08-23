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

  def toProvider(provider: ObjectType => A): Design = {
    design.addBinding(ProviderBinding(from, provider))
  }

  def toProvider[D1 : ru.TypeTag](factory: D1 => A): Design = {
    design.addBinding(FactoryBinding(from, ObjectType.of(implicitly[ru.TypeTag[D1]].tpe), factory))
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
}

