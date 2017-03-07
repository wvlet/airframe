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

import scala.language.experimental.macros

/**
  *
  */
object Frame {
  import scala.collection.JavaConverters._
  type FullName = String

  private[frame] val frameCache = new ConcurrentHashMap[FullName, Frame]().asScala

  def of[A]: Frame = macro FrameMacros.of[A]

}

trait Frame {
  def name = rawType.getSimpleName
  def fullName: String = rawType.getName
  def rawType: Class[_]
  def params: Seq[Param] = Seq.empty

  override def toString = {
    if(params.isEmpty) {
      name
    }
    else {
      s"${name}(${params.mkString(",")})"
    }
  }
  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case f:Frame =>
        this.fullName.equals(f.fullName)
      case _ => false
    }
  }
  override def hashCode(): Int = fullName.hashCode
}

case class Param(name: String, frame: Frame) {
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

case class Alias(override val name: String, override val fullName: String, frame: Frame) extends Frame {
  override def toString = s"${name}:=${frame.toString}"
  override def rawType = frame.rawType
  override def params = frame.params
}

class GenericFrame(val rawType: Class[_], typeArgs: Seq[Frame]) extends Frame {
  override def toString = s"${name}[${typeArgs.map(_.name).mkString(",")}]"
  override def fullName = s"${rawType.getName}[${typeArgs.map(_.fullName).mkString(",")}]"
}

case class ClassFrame(val rawType: Class[_]) extends Frame

case class ArrayFrame(override val rawType: Class[_], elementFrame: Frame) extends GenericFrame(rawType, Seq(elementFrame)) {
  override def toString = s"Array[${elementFrame.name}]"
  override def fullName = s"Array[${elementFrame.fullName}]"
}
case class OptionFrame(override val rawType: Class[_], elementFrame: Frame) extends GenericFrame(rawType, Seq(elementFrame))

case class EnumFrame(override val rawType: Class[_]) extends Frame

