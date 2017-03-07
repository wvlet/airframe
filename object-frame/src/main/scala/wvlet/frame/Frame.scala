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


trait Frame {
  def name = rawType.getSimpleName
  def fullName : FullName = rawType.getName
  def rawType:Class[_]
  def params:Seq[Param] =Seq.empty

  override def toString = s"${name}(${params.mkString(",")})"
}

case class Param(name:String, frame:Frame) {
  override def toString = s"${name}:${frame.name}"
}

object Primitive {

  case object Int extends Frame {
    override def name = "Int"
    def rawType: Class[Int] = classOf[Int]
  }
  case object Byte extends Frame {
    override def name = "Byte"
    def rawType: Class[Byte] = classOf[Byte]
  }
  case object Long extends Frame {
    override def name = "Long"
    def rawType: Class[Long] = classOf[Long]
  }
  case object Short extends Frame {
    override def name = "Short"
    def rawType: Class[Short] = classOf[Short]
  }
  case object Boolean extends Frame {
    override def name = "Boolean"
    def rawType: Class[Boolean] = classOf[Boolean]
  }
  case object Float extends Frame {
    override def name = "Float"
    def rawType: Class[Float] = classOf[Float]
  }
  case object Double extends Frame {
    override def name = "Double"
    def rawType: Class[Double] = classOf[Double]
  }
  case object String extends Frame {
    def rawType: Class[String] = classOf[String]
  }

}

object StandardType {

}

case class ObjectFrame(rawType:Class[_]) extends Frame

case class Alias(override val name:String, override val fullName:FullName, frame:Frame) extends Frame {
  override def toString = s"${name}:=${frame.toString}"
  override def rawType = frame.rawType
  override def params = frame.params
}

class GenericFrame(val rawType:Class[_], typeArgs:Seq[Frame]) extends Frame {
  override def toString = s"${name}[${typeArgs.map(_.name).mkString(",")}]"
  override def fullName = s"${rawType.getName}[${typeArgs.map(_.fullName).mkString(",")}]"
}

case class ArrayFrame(override val rawType:Class[_], elementFrame:Frame) extends GenericFrame(rawType, Seq(elementFrame)) {
  override def toString = s"Array[${elementFrame.name}]"
  override def fullName = s"Array[${elementFrame.fullName}]"
}
case class SeqFrame(override val rawType:Class[_], elementFrame:Frame) extends GenericFrame(rawType, Seq(elementFrame))
case class SetFrame(override val rawType:Class[_], elementFrame:Frame) extends GenericFrame(rawType, Seq(elementFrame))
case class ListFrame(override val rawType:Class[_], elementFrame:Frame) extends GenericFrame(rawType, Seq(elementFrame))
case class OptionFrame(override val rawType:Class[_], elementFrame:Frame) extends GenericFrame(rawType, Seq(elementFrame))
case class MapFrame(override val rawType:Class[_], keyFrame:Frame, valueFrame:Frame) extends GenericFrame(rawType, Seq(keyFrame, valueFrame))
