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

import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}
import scala.language.experimental.macros

/**
  *
  */
object Frame {
  import scala.collection.JavaConverters._

  private[frame] val frameCache = new ConcurrentHashMap[Class[_], Frame[_]]().asScala

  def of[A:ru.TypeTag] : Frame[A] = macro FrameMacros.of[A]

}

case class Param(name:String, frame:Frame[_]) {
  override def toString = s"${name}:${frame.name}"
}

case object IntFrame extends Frame[Int] {
  def cl : Class[Int] = classOf[Int]
}
case object ByteFrame extends Frame[Byte] {
  def cl : Class[Byte] = classOf[Byte]
}
case object LongFrame extends Frame[Long] {
  def cl : Class[Long] = classOf[Long]
}
case object ShortFrame extends Frame[Short] {
  def cl : Class[Short] = classOf[Short]
}
case object BooleanFrame extends Frame[Boolean]{
  def cl : Class[Boolean] = classOf[Boolean]
}
case object FloatFrame extends Frame[Float]{
  def cl : Class[Float] = classOf[Float]
}
case object DoubleFrame extends Frame[Double]{
  def cl : Class[Double] = classOf[Double]
}
case object StringFrame extends Frame[String]{
  def cl : Class[String] = classOf[String]
}
case class ObjectFrame(cl:Class[_]) extends Frame[Any]

trait Frame[A] {
  def name = cl.getSimpleName
  def cl:Class[_]
  def params:Seq[Param] =Seq.empty

  override def toString = s"Frame[${cl.getSimpleName}](${params.mkString(",")})"
}
