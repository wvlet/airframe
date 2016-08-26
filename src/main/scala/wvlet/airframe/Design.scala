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

import wvlet.airframe.Binder.Binding
import wvlet.log.LogSupport
import wvlet.obj.ObjectType

import scala.language.experimental.macros
import scala.reflect.runtime.{universe => ru}

/**
  * Immutable airframe design
  */
case class Design(binding: Vector[Binding]) extends LogSupport {

  def +(other: Design): Design = {
    new Design(binding ++ other.binding)
  }

  def bind[A:ru.TypeTag]: Binder[A] = {
    bind(ObjectType.of(implicitly[ru.TypeTag[A]].tpe)).asInstanceOf[Binder[A]]
  }

  def bind(t: ObjectType): Binder[Any] = {
    val b = new Binder[Any](this, t)
    b
  }

  private[airframe] def addBinding(b: Binding): Design = {
    debug(s"Add binding: $b")
    new Design(binding :+ b)
  }

  def remove[A:ru.TypeTag] : Design = {
    val target = ObjectType.of(implicitly[ru.TypeTag[A]].tpe)
    new Design(binding.filterNot(_.from == target))
  }

  def session: SessionBuilder = {
    new SessionBuilder(this)
  }

  def newSession : Session = {
    new SessionBuilder(this).create
  }
}

object Design {
  val blanc: Design = new Design(Vector.empty)
}
