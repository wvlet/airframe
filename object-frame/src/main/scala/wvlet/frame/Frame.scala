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
package wvlet.frame

import java.util.concurrent.ConcurrentHashMap

import wvlet.frame.Frame.FullName

import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}
import scala.language.experimental.macros

/**
  *
  */
object Frame {
  import scala.collection.JavaConverters._

  type FullName = String

  private[frame] val frameCache = new ConcurrentHashMap[FullName, Frame]().asScala

  def of[A] : Frame = macro FrameMacros.of[A]

}

case class Param(name:String, frame:Frame) {
  override def toString = s"${name}:${frame.name}"
}

case object IntFrame extends Frame {
  def cl : Class[Int] = classOf[Int]
}
case object ByteFrame extends Frame {
  def cl : Class[Byte] = classOf[Byte]
}
case object LongFrame extends Frame {
  def cl : Class[Long] = classOf[Long]
}
case object ShortFrame extends Frame {
  def cl : Class[Short] = classOf[Short]
}
case object BooleanFrame extends Frame {
  def cl : Class[Boolean] = classOf[Boolean]
}
case object FloatFrame extends Frame {
  def cl : Class[Float] = classOf[Float]
}
case object DoubleFrame extends Frame {
  def cl : Class[Double] = classOf[Double]
}
case object StringFrame extends Frame {
  def cl : Class[String] = classOf[String]
}
case class ObjectFrame(cl:Class[_]) extends Frame

case class FrameAlias(override val name:String, override val fullName:FullName, frame:Frame) extends Frame {
  override def cl = frame.cl
  override def params = frame.params
}

trait Frame {
  def name = cl.getSimpleName
  def fullName : FullName = cl.getName
  def cl:Class[_]
  def params:Seq[Param] =Seq.empty

  override def toString = s"Frame[${name}](${params.mkString(",")})"
}
