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
package wvlet.airframe.surface

import scala.language.existentials

sealed trait ParameterBase extends Serializable {
  def name: String
  def surface: Surface

  def call(obj: Any, x: Any*): Any
}

trait Parameter extends ParameterBase {
  def index: Int
  def name: String

  /**
    * Surface for representing this parameter type
    */
  def surface: Surface

  /**
    * Returns true if this parameter has @required annotation
    */
  def isRequired: Boolean

  /**
    * Returns true if this parameter has @required annotation
    */
  def isSecret: Boolean

  /**
    * Get this parameter value from a given object x
    */
  def get(x: Any): Any

  override def call(obj: Any, x: Any*): Any = get(obj)

  /**
    * Get the default value of this parameter. For example the default value of x in class A(x:Int = 10) is 10
    *
    * @return
    */
  def getDefaultValue: Option[Any]
}

/**
  */
trait ObjectFactory extends Serializable {
  def newInstance(args: Seq[Any]): Any
}

case class MethodRef(owner: Class[_], name: String, paramTypes: Seq[Class[_]], isConstructor: Boolean)

trait MethodParameter extends Parameter {
  def method: MethodRef

  /**
    * Method owner instance is necessary to find by-name parameter default values
    * @param methodOwner
    * @return
    */
  def getMethodArgDefaultValue(methodOwner: Any): Option[Any] = getDefaultValue
}

trait MethodSurface extends ParameterBase {
  def mod: Int
  def owner: Surface
  def name: String
  def args: Seq[MethodParameter]
  def surface: Surface = returnType
  def returnType: Surface

  def isPublic: Boolean    = (mod & MethodModifier.PUBLIC) != 0
  def isPrivate: Boolean   = (mod & MethodModifier.PRIVATE) != 0
  def isProtected: Boolean = (mod & MethodModifier.PROTECTED) != 0
  def isStatic: Boolean    = (mod & MethodModifier.STATIC) != 0
  def isFinal: Boolean     = (mod & MethodModifier.FINAL) != 0
  def isAbstract: Boolean  = (mod & MethodModifier.ABSTRACT) != 0
}

/**
  * Parameters of a Surface. Renamed from StdMethodParameter to StaticMethodParameter for binary compatibility
  */
case class StaticMethodParameter(
    method: MethodRef,
    index: Int,
    name: String,
    isRequired: Boolean,
    isSecret: Boolean,
    surface: Surface,
    private val defaultValue: Option[Any] = None,
    accessor: Option[Any => Any] = None,
    methodArgAccessor: Option[Any => Any] = None
) extends MethodParameter {
  override def toString: String             = s"${name}:${surface.name}"
  def get(x: Any): Any                      = accessor.map(a => a(x)).getOrElse(null)
  override def getDefaultValue: Option[Any] = defaultValue
  override def getMethodArgDefaultValue(methodOwner: Any): Option[Any] = {
    methodArgAccessor.map { acc =>
      acc(methodOwner)
    }
  }
}

object Primitive {
  import java.{lang => jl}

  private[surface] val primitiveTable = {
    val b = Map.newBuilder[Class[_], PrimitiveSurface]
    b += classOf[jl.Boolean]           -> Boolean
    b += classOf[Boolean]              -> Boolean
    b += classOf[jl.Short]             -> Short
    b += classOf[Short]                -> Short
    b += classOf[jl.Byte]              -> Byte
    b += classOf[Byte]                 -> Byte
    b += classOf[jl.Character]         -> Char
    b += classOf[Char]                 -> Char
    b += classOf[jl.Integer]           -> Int
    b += classOf[Int]                  -> Int
    b += classOf[jl.Float]             -> Float
    b += classOf[Float]                -> Float
    b += classOf[jl.Long]              -> Long
    b += classOf[Long]                 -> Long
    b += classOf[jl.Double]            -> Double
    b += classOf[Double]               -> Double
    b += classOf[jl.String]            -> String
    b += classOf[String]               -> String
    b += classOf[BigInt]               -> BigInt
    b += classOf[java.math.BigInteger] -> BigInteger
    b.result()
  }

  def apply(cl: Class[_]): PrimitiveSurface = primitiveTable(cl)

  sealed abstract class PrimitiveSurface(rawType: Class[_]) extends GenericSurface(rawType) {
    override def isPrimitive: Boolean = true
  }

  case object Int extends PrimitiveSurface(classOf[Int]) {
    override def name: String     = "Int"
    override def fullName: String = "Int"
  }
  case object Byte extends PrimitiveSurface(classOf[Byte]) {
    override def name: String     = "Byte"
    override def fullName: String = "Byte"
  }
  case object Long extends PrimitiveSurface(classOf[Long]) {
    override def name: String     = "Long"
    override def fullName: String = "Long"
  }
  case object Short extends PrimitiveSurface(classOf[Short]) {
    override def name: String     = "Short"
    override def fullName: String = "Short"
  }
  case object Boolean extends PrimitiveSurface(classOf[Boolean]) {
    override def name: String     = "Boolean"
    override def fullName: String = "Boolean"
  }
  case object Float extends PrimitiveSurface(classOf[Float]) {
    override def name: String     = "Float"
    override def fullName: String = "Float"
  }
  case object Double extends PrimitiveSurface(classOf[Double]) {
    override def name: String     = "Double"
    override def fullName: String = "Double"
  }
  case object Char extends PrimitiveSurface(classOf[Char]) {
    override def name: String     = "Char"
    override def fullName: String = "Char"
  }
  case object String extends PrimitiveSurface(classOf[String])

  case object Unit extends PrimitiveSurface(classOf[Unit]) {
    override def name: String     = "Unit"
    override def fullName: String = "Unit"
  }
  case object BigInt     extends PrimitiveSurface(classOf[BigInt])
  case object BigInteger extends PrimitiveSurface(classOf[java.math.BigInteger])
}

case class Alias(override val name: String, override val fullName: String, ref: Surface)
    extends GenericSurface(ref.rawType, ref.typeArgs, ref.params, ref.objectFactory) {
  override def toString: String     = s"${name}:=${ref.name}"
  override def isAlias: Boolean     = true
  override def isPrimitive: Boolean = ref.isPrimitive
  override def isOption: Boolean    = ref.isOption
  override def dealias: Surface     = ref.dealias
}

case class HigherKindedTypeSurface(
    override val name: String,
    override val fullName: String,
    ref: Surface,
    override val typeArgs: Seq[Surface]
) extends GenericSurface(ref.rawType, typeArgs, ref.params, ref.objectFactory) {
  override def toString: String = {
    val s = if (typeArgs.isEmpty) {
      name
    } else {
      s"${name}[${typeArgs.mkString(",")}]"
    }
    TypeName.sanitizeTypeName(s)
  }
  override def isAlias: Boolean     = false
  override def isPrimitive: Boolean = ref.isPrimitive
  override def isOption: Boolean    = ref.isOption
  override def dealias: Surface     = ref.dealias
}

case object ExistentialType extends GenericSurface(classOf[Any]) {
  override def name: String     = "_"
  override def fullName: String = "_"
}

case class ArraySurface(override val rawType: Class[_], elementSurface: Surface)
    extends GenericSurface(rawType, Seq(elementSurface)) {
  override def name: String     = s"Array[${elementSurface.name}]"
  override def fullName: String = s"Array[${elementSurface.fullName}]"
  override def toString: String = name
}

case class OptionSurface(override val rawType: Class[_], elementSurface: Surface)
    extends GenericSurface(rawType, Seq(elementSurface)) {
  override def isOption: Boolean = true
}
case class JavaEnumSurface(override val rawType: Class[_]) extends GenericSurface(rawType)

case class EnumSurface(override val rawType: Class[_], stringExtractor: (Class[_], String) => Option[Any])
    extends GenericSurface(rawType)

case class TupleSurface(override val rawType: Class[_], override val typeArgs: Seq[Surface])
    extends GenericSurface(rawType, typeArgs) {}

case class TaggedSurface(base: Surface, tag: Surface) extends Surface {
  override def toString: String       = name
  override def rawType: Class[_]      = base.rawType
  override def typeArgs: Seq[Surface] = base.typeArgs
  override def params: Seq[Parameter] = base.params
  override def name: String           = s"${base.name}@@${tag.name}"
  override def fullName: String       = s"${base.fullName}@@${tag.fullName}"
  override def isOption: Boolean      = base.isOption
  override def isAlias: Boolean       = base.isAlias
  override def isPrimitive: Boolean   = base.isPrimitive
  override def dealias: Surface       = base.dealias

  override def objectFactory: Option[ObjectFactory] = base.objectFactory
}

case object AnyRefSurface extends GenericSurface(classOf[AnyRef]) {
  override def name: String = "AnyRef"
}

/**
  * Base class for generic surfaces with type args
  *
  * @param rawType
  * @param typeArgs
  * @param params
  * @param objectFactory
  */
class GenericSurface(
    val rawType: Class[_],
    val typeArgs: Seq[Surface] = Seq.empty,
    val params: Seq[Parameter] = Seq.empty,
    override val objectFactory: Option[ObjectFactory] = None
) extends Surface {
  private def getClassName: String = {
    try {
      TypeName.sanitizeTypeName(rawType.getSimpleName)
    } catch {
      case e: InternalError =>
        // Scala REPL use class name like $line3.$read$$iw$$iw$A, which causes InternalError at getSimpleName
        TypeName.sanitizeTypeName(rawType.getName)
    }
  }

  def name: String = {
    val clsName = TypeName.sanitizeTypeName(getClassName)
    val s = if (typeArgs.isEmpty) {
      clsName
    } else {
      s"${clsName}[${typeArgs.map(_.name).mkString(",")}]"
    }
    TypeName.sanitizeTypeName(s)
  }

  def fullName: String = {
    val clsName = TypeName.sanitizeTypeName(rawType.getName)
    val s = if (typeArgs.isEmpty) {
      clsName
    } else {
      s"${clsName}[${typeArgs.map(_.fullName).mkString(",")}]"
    }
    TypeName.sanitizeTypeName(s)
  }

  def isOption: Boolean    = false
  def isAlias: Boolean     = false
  def isPrimitive: Boolean = false

  override def toString: String = name

  override def equals(obj: Any): Boolean = {
    obj match {
      case f: Surface =>
        this.fullName.equals(f.fullName)
      case _ => false
    }
  }

  override def hashCode(): Int = fullName.hashCode
}

/**
  * Surface placeholder for supporting recursive types
  * @param rawType
  */
case class LazySurface(rawType: Class[_], fullName: String) extends Surface {
  // Resolved the final type from the full surface name
  protected def ref: Surface = wvlet.airframe.surface.getCached(fullName)

  def name: String = {
    val s = if (typeArgs.isEmpty) {
      rawType.getSimpleName
    } else {
      s"${rawType.getSimpleName}[${typeArgs.map(_.name).mkString(",")}]"
    }
    TypeName.sanitizeTypeName(s)
  }

  override def toString: String                     = name
  override def params                               = ref.params
  override def typeArgs: Seq[Surface]               = ref.typeArgs
  override def isOption                             = ref.isOption
  override def isAlias                              = ref.isAlias
  override def isPrimitive                          = ref.isPrimitive
  override def objectFactory: Option[ObjectFactory] = ref.objectFactory
}

case class ClassMethodSurface(
    mod: Int,
    owner: Surface,
    name: String,
    returnType: Surface,
    args: Seq[MethodParameter],
    methodCaller: Option[(Any, Seq[Any]) => Any]
) extends MethodSurface {
  override def call(obj: Any, x: Any*) = {
    def unsupported =
      throw new UnsupportedOperationException(s"Calling method ${name} is not supported: ${this}")

    methodCaller
      .map { caller =>
        caller(obj, x.toSeq)
      }
      .getOrElse {
        unsupported
      }
  }
}
