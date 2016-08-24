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

object Design {
  val blanc: Design = new Design(Seq.empty)
}