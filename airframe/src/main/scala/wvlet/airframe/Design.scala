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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import wvlet.airframe.Binder.Binding
import wvlet.log.LogSupport
import wvlet.surface.Surface

import scala.language.experimental.macros
import scala.reflect.runtime.{universe => ru}


/**
  * Immutable airframe design
  */
case class Design(binding: Vector[Binding]) extends LogSupport {

  def +(other: Design): Design = {
    new Design(binding ++ other.binding)
  }

  def bind[A]: Binder[A] = macro AirframeMacros.designBindImpl[A]

  def bind(t: Surface): Binder[Any] = {
    val b = new Binder[Any](this, t)
    b
  }

  private[airframe] def addBinding(b: Binding): Design = {
    debug(s"Add binding: $b")
    new Design(binding :+ b)
  }

  def remove[A] : Design = {
    val target = Surface.of[A]
    new Design(binding.filterNot(_.from == target))
  }

  def session: SessionBuilder = {
    new SessionBuilder(this)
  }

  def newSession : Session = {
    new SessionBuilder(this).create
  }

  def withSession[U](body:Session => U) : U = {
    val session = newSession
    try {
      session.start
      body(session)
    }
    finally {
      session.shutdown
    }
  }

  override def toString : String = {
    s"Design:\n ${binding.mkString("\n ")}"
  }

  private[airframe] def serialize : Array[Byte] = {
    val b = new ByteArrayOutputStream()
    val oo = new ObjectOutputStream(b)
    oo.writeObject(this)
    oo.close()
    b.toByteArray
  }

}

object Design {
  /**
    * Empty design.
    */
  val blanc: Design = new Design(Vector.empty) // Use Vector for better append performance

  implicit class DesignAccess(design: Design) {
    def addBinding(b:Binding) = design.addBinding(b)
  }

  private[airframe] def deserialize(b: Array[Byte]) : Design = {
    val in = new ByteArrayInputStream(b)
    val oi = new ObjectInputStream(in)
    val obj = oi.readObject().asInstanceOf[Design]
    obj.asInstanceOf[Design]
  }
}
