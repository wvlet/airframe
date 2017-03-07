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
package wvlet.surface

import java.util.concurrent.ConcurrentHashMap

import scala.language.experimental.macros

/**
  *
  */
object Surface {
  import scala.collection.JavaConverters._
  type FullName = String

  private[surface] val surfaceCache = new ConcurrentHashMap[FullName, Surface]().asScala

  def of[A]: Surface = macro SurfaceMacros.of[A]

}

trait Surface {
  def name = rawType.getSimpleName
  def fullName: String = {
    if(typeArgs.isEmpty)
      rawType.getName
    else {
      s"${rawType.getName}[${typeArgs.map(_.fullName).mkString(",")}]"
    }
  }
  def rawType: Class[_]
  def typeArgs : Seq[Surface] = Seq.empty
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
      case f:Surface =>
        this.fullName.equals(f.fullName)
      case _ => false
    }
  }
  override def hashCode(): Int = fullName.hashCode
}

case class Param(name: String, surface: Surface) {
  override def toString = s"${name}:${surface.name}"
}

object Primitive {

  case object Int extends Surface {
    override def name = "Int"
    def rawType: Class[Int] = classOf[Int]
  }
  case object Byte extends Surface {
    override def name = "Byte"
    def rawType: Class[Byte] = classOf[Byte]
  }
  case object Long extends Surface {
    override def name = "Long"
    def rawType: Class[Long] = classOf[Long]
  }
  case object Short extends Surface {
    override def name = "Short"
    def rawType: Class[Short] = classOf[Short]
  }
  case object Boolean extends Surface {
    override def name = "Boolean"
    def rawType: Class[Boolean] = classOf[Boolean]
  }
  case object Float extends Surface {
    override def name = "Float"
    def rawType: Class[Float] = classOf[Float]
  }
  case object Double extends Surface {
    override def name = "Double"
    def rawType: Class[Double] = classOf[Double]
  }
  case object String extends Surface {
    def rawType: Class[String] = classOf[String]
  }

}

case class Alias(override val name: String, override val fullName: String, frame: Surface) extends Surface {
  override def toString = s"${name}:=${frame.toString}"
  override def rawType = frame.rawType
  override def params = frame.params
}

class GenericSurface(val rawType: Class[_], override val typeArgs: Seq[Surface]) extends Surface {
  override def toString = s"${name}[${typeArgs.map(_.name).mkString(",")}]"
  override def fullName = s"${rawType.getName}[${typeArgs.map(_.fullName).mkString(",")}]"
}

case object ExistentialType extends Surface {
  override def name = "_"
  override def fullName = "_"
  override def rawType = classOf[Any]
}

case class ClassSurface(val rawType: Class[_]) extends Surface

case class ArraySurface(override val rawType: Class[_], elementSurface: Surface) extends GenericSurface(rawType, Seq(elementSurface)) {
  override def toString = s"Array[${elementSurface.name}]"
  override def fullName = s"Array[${elementSurface.fullName}]"
}
case class OptionSurface(override val rawType: Class[_], elementSurface: Surface) extends GenericSurface(rawType, Seq(elementSurface))

case class EnumSurface(override val rawType: Class[_]) extends Surface
case class TupleSurface(override val rawType: Class[_], override val typeArgs:Seq[Surface]) extends GenericSurface(rawType, typeArgs)
