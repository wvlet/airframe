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

import scala.language.existentials
import scala.language.experimental.macros

/**
  *
  */
object Surface {

  import scala.collection.JavaConverters._

  type FullName = String

  private[surface] val surfaceCache       = new ConcurrentHashMap[FullName, Surface]().asScala
  private[surface] val methodSurfaceCache = new ConcurrentHashMap[FullName, Seq[MethodSurface]]().asScala

  def of[A]: Surface = macro SurfaceMacros.of[A]
  def methodsOf[A]: Seq[MethodSurface] = macro SurfaceMacros.methodsOf[A]
}

case class Annotation(tpe: Surface, params: Map[_, _])

trait Constructor {
  def newInstance(args:Seq[Any]) : Any
}

trait Surface {
  def rawType: Class[_]
  def typeArgs: Seq[Surface]
  def params: Seq[Parameter]
  def name: String
  def fullName: String
  def annotations: Seq[Annotation]

  def isOption: Boolean
  def isAlias: Boolean
  def isPrimitive: Boolean

  def getConstructor: Option[Constructor] = None
}

class GenericSurface(
  val rawType: Class[_],
  val typeArgs: Seq[Surface] = Seq.empty,
  val params: Seq[Parameter] = Seq.empty,
  val annotations: Seq[Annotation] = Seq.empty) extends Surface {

  def name: String = {
    if (typeArgs.isEmpty) {
      rawType.getSimpleName
    }
    else {
      s"${rawType.getSimpleName}[${typeArgs.map(_.name).mkString(",")}]"
    }
  }

  def fullName: String = {
    if (typeArgs.isEmpty) {
      rawType.getName
    }
    else {
      s"${rawType.getName}[${typeArgs.map(_.fullName).mkString(",")}]"
    }
  }

  def isOption: Boolean = false
  def isAlias: Boolean = false
  def isPrimitive: Boolean = false

  override def toString: String = {
    if (params.isEmpty) {
      name
    }
    else {
      s"${name}(${params.mkString(",")})"
    }
  }
  override def equals(obj: Any): Boolean = {
    obj match {
      case f: Surface =>
        this.fullName.equals(f.fullName)
      case _ => false
    }
  }

  override def hashCode(): Int = fullName.hashCode
}

case class Parameter(name: String, surface: Surface, annotations:Seq[Annotation]= Seq.empty) {
  override def toString = s"${name}:${surface.name}"
}

object Primitive {

  sealed abstract class PrimitiveSurface(rawType: Class[_]) extends GenericSurface(rawType) {
    override def isPrimitive: Boolean = true
  }

  case object Int extends PrimitiveSurface(classOf[Int]) {
    override def name: String = "Int"
    override def fullName: String = "Int"
  }
  case object Byte extends PrimitiveSurface(classOf[Byte]) {
    override def name: String = "Byte"
    override def fullName: String = "Byte"
  }
  case object Long extends PrimitiveSurface(classOf[Long]) {
    override def name: String = "Long"
    override def fullName: String = "Long"
  }
  case object Short extends PrimitiveSurface(classOf[Short]) {
    override def name: String = "Short"
    override def fullName: String = "Short"
  }
  case object Boolean extends PrimitiveSurface(classOf[Boolean]) {
    override def name: String = "Boolean"
    override def fullName: String = "Boolean"
  }
  case object Float extends PrimitiveSurface(classOf[Float]) {
    override def name: String = "Float"
    override def fullName: String = "Float"
  }
  case object Double extends PrimitiveSurface(classOf[Double]) {
    override def name: String = "Double"
    override def fullName: String = "Double"
  }
  case object String extends PrimitiveSurface(classOf[String])
}

case class Alias(override val name: String, override val fullName: String, ref: Surface) extends GenericSurface(ref.rawType, ref.typeArgs, ref.params) {
  override def toString: String = s"${name}:=${ref.name}"
  override def isAlias: Boolean = true
  override def getConstructor: Option[Constructor] = ref.getConstructor
}

case object ExistentialType extends GenericSurface(classOf[Any]) {
  override def name: String = "_"
  override def fullName: String = "_"
}

case class ArraySurface(override val rawType: Class[_], elementSurface: Surface) extends GenericSurface(rawType, Seq(elementSurface)) {
  override def name: String = s"Array[${elementSurface.name}]"
  override def fullName: String = s"Array[${elementSurface.fullName}]"
  override def toString: String = name
}

case class OptionSurface(override val rawType: Class[_], elementSurface: Surface) extends GenericSurface(rawType, Seq(elementSurface)) {
  override def isOption: Boolean = true
}
case class EnumSurface(override val rawType: Class[_]) extends GenericSurface(rawType) {
}
case class TupleSurface(override val rawType: Class[_], override val typeArgs: Seq[Surface]) extends GenericSurface(rawType, typeArgs) {
}

trait MethodSurface {
  def mod: Int
  def owner: Surface
  def name: String
  def args: Seq[Parameter]
  def returnType: Surface
  def annotations: Seq[Annotation]

  def isPublic: Boolean = (mod & MethodModifier.PUBLIC) != 0
  def isPrivate: Boolean = (mod & MethodModifier.PRIVATE) != 0
  def isProtected: Boolean = (mod & MethodModifier.PROTECTED) != 0
  def isStatic: Boolean = (mod & MethodModifier.STATIC) != 0
  def isFinal: Boolean = (mod & MethodModifier.FINAL) != 0
  def isAbstract: Boolean = (mod & MethodModifier.ABSTRACT) != 0
}

case class ClassMethod(mod: Int, owner: Surface, name: String, returnType: Surface, args: Seq[Parameter], annotations: Seq[Annotation]) extends MethodSurface {
}

//case class CompanionMethodSurface(owner:)

object MethodModifier {
  val PUBLIC       = 0x00000001
  val PRIVATE      = 0x00000002
  val PROTECTED    = 0x00000004
  val STATIC       = 0x00000008
  val FINAL        = 0x00000010
  val SYNCHRONIZED = 0x00000020
  val VOLATILE     = 0x00000040
  val TRANSIENT    = 0x00000080
  val NATIVE       = 0x00000100
  val INTERFACE    = 0x00000200
  val ABSTRACT     = 0x00000400
  val STRICT       = 0x00000800
}
