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
package wvlet.surface.reflect

import java.util.concurrent.ConcurrentHashMap

import wvlet.log.LogSupport
import wvlet.surface._

import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}
import scala.util.Try
import scala.collection.JavaConverters._

/**
  *
  */
object RuntimeSurface extends LogSupport {

  private type SurfaceFactory = PartialFunction[ru.Type, Surface]

  private val cache = new ConcurrentHashMap[ru.Type, Surface]().asScala

  private[reflect] def mirror = ru.runtimeMirror(Thread.currentThread.getContextClassLoader)

  private def resolveClass(tpe: ru.Type): Class[_] = {
    try {
      mirror.runtimeClass(tpe)
    }
    catch {
      case e: Throwable => classOf[Any]
    }
  }

  def of[A: ru.WeakTypeTag]: Surface = {
    val tag = implicitly[ru.WeakTypeTag[A]]
    val tpe = tag.tpe
    cache.getOrElseUpdate(tpe, apply(tpe))
  }

  def apply(tpe: ru.Type): Surface = {
    cache.getOrElseUpdate(tpe, new SurfaceFinder().find(tpe))
  }

  private class SurfaceFinder extends LogSupport {
    private val seen = scala.collection.mutable.Set[ru.Type]()

    private def surfaceOf(tpe: ru.Type): Surface = apply(tpe)

    def find(tpe: ru.Type): Surface = {
      if (seen.contains(tpe)) {
        throw new IllegalStateException(s"Cyclic reference for ${tpe}")
      }
      else {
        seen += tpe
        val m = surfaceFactories.orElse[ru.Type, Surface] {
          case _ =>
            trace(f"Resolving the unknown type $tpe into AnyRef")
            new GenericSurface(resolveClass(tpe))
        }
        m(tpe)
      }
    }

    private val surfaceFactories: SurfaceFactory =
      taggedTypeFactory orElse
        aliasFactory orElse
        primitiveTypeFactory orElse
        arrayFactory orElse
        optionFactory orElse
        tupleFactory orElse
        javaUtilFactory orElse
        enumFactory orElse
        genericSurfaceWithConstructorFactory orElse
        existentialTypeFactory orElse
        genericSurfaceFactory

    private def primitiveTypeFactory: SurfaceFactory = {
      case t if t =:= typeOf[String] => Primitive.String
      case t if t =:= typeOf[Boolean] => Primitive.Boolean
      case t if t =:= typeOf[Int] => Primitive.Int
      case t if t =:= typeOf[Long] => Primitive.Long
      case t if t =:= typeOf[Float] => Primitive.Float
      case t if t =:= typeOf[Double] => Primitive.Double
      case t if t =:= typeOf[Short] => Primitive.Short
      case t if t =:= typeOf[Byte] => Primitive.Byte
      case t if t =:= typeOf[Char] => Primitive.Char
    }

    private def typeNameOf(t: ru.Type): String = {
      t.dealias.typeSymbol.fullName
    }

    private def typeArgsOf(t: ru.Type): List[ru.Type] = t match {
      case TypeRef(prefix, symbol, args) =>
        args
      case ru.ExistentialType(quantified, underlying) =>
        typeArgsOf(underlying)
      case other =>
        List.empty
    }

    private def elementTypeOf(t: ru.Type): Surface = {
      typeArgsOf(t).map(surfaceOf(_)).head
    }

    private def taggedTypeFactory: SurfaceFactory = {
      case t if t.typeArgs.length == 2 && typeNameOf(t).startsWith("wvlet.surface.tag.") =>
        TaggedSurface(surfaceOf(t.typeArgs(0)), surfaceOf(t.typeArgs(1)))
    }

    private def belongsToScalaDefault(t: ru.Type) = {
      t match {
        case ru.TypeRef(prefix, _, _) =>
          val scalaDefaultPackages = Seq("scala.", "scala.Predef.", "scala.util.")
          scalaDefaultPackages.exists(p => prefix.dealias.typeSymbol.fullName.startsWith(p))
        case _ => false
      }
    }

    private def aliasFactory: SurfaceFactory = {
      case alias@TypeRef(prefix, symbol, args)
        if symbol.isType &&
          symbol.asType.isAliasType &&
          !belongsToScalaDefault(alias) =>
        val inner = surfaceOf(alias.dealias)
        val name = symbol.asType.name.decodedName.toString
        val fullName = s"${prefix.typeSymbol.fullName}.${name}"
        Alias(name, fullName, inner)
    }

    private def arrayFactory: SurfaceFactory = {
      case t if typeNameOf(t) == "scala.Array" =>
        ArraySurface(resolveClass(t), elementTypeOf(t))
    }

    private def optionFactory: SurfaceFactory = {
      case t if typeNameOf(t) == "scala.Option" =>
        OptionSurface(resolveClass(t), elementTypeOf(t))
    }

    private def tupleFactory: SurfaceFactory = {
      case t if t <:< typeOf[Product] && t.typeSymbol.fullName.startsWith("scala.Tuple") =>
        val paramType = typeArgsOf(t).map(x => surfaceOf(x))
        TupleSurface(resolveClass(t), paramType.toIndexedSeq)
    }

    private def javaUtilFactory: SurfaceFactory = {
      case t if
      t =:= typeOf[java.io.File] ||
        t =:= typeOf[java.util.Date] ||
        t =:= typeOf[java.time.temporal.Temporal] ||
        t =:= typeOf[Throwable] ||
        t =:= typeOf[Exception] ||
        t =:= typeOf[Error]
      =>
        new GenericSurface(resolveClass(t))
    }

    private def isEnum(t: ru.Type): Boolean = {
      t.baseClasses.exists(x =>
        x.isJava && x.isType && x.asType.name.decodedName.toString.startsWith("java.lang.Enum")
      )
    }

    private def enumFactory: SurfaceFactory = {
      case t if isEnum(t) =>
        EnumSurface(resolveClass(t))
    }

    def hasAbstractMethods(t: ru.Type): Boolean = t.members.exists(x =>
      x.isMethod && x.isAbstract && !x.isAbstractOverride
    )

    private def isAbstract(t: ru.Type): Boolean = {
      t.typeSymbol.isAbstract && hasAbstractMethods(t)
    }

    private def isPhantomConstructor(constructor: Symbol): Boolean =
      constructor.asMethod.fullName.endsWith("$init$")

    def publicConstructorsOf(t: ru.Type): Iterable[MethodSymbol] = {
      t.members
      .filter(m => m.isMethod && m.asMethod.isConstructor && m.isPublic)
      .filterNot(isPhantomConstructor)
      .map(_.asMethod)
    }

    def findPrimaryConstructorOf(t: ru.Type): Option[MethodSymbol] = {
      publicConstructorsOf(t).find(x => x.isPrimaryConstructor)
    }

    case class MethodArg(paramName: Symbol, tpe: ru.Type) {
      def name: String = paramName.name.decodedName.toString
      def typeSurface: Surface = surfaceOf(tpe)
    }

    private def findMethod(m: ru.Type, name: String): Option[MethodSymbol] = {
      m.member(ru.TermName(name)) match {
        case ru.NoSymbol => None
        case other => Some(other.asMethod)
      }
    }

    private def methodArgsOf(targetType: ru.Type, constructor: MethodSymbol): List[List[MethodArg]] = {
      val classTypeParams = targetType.typeSymbol.asClass.typeParams

      val companion = targetType.companion match {
        case NoType => None
        case comp => Some(comp)
      }

      for (params <- constructor.paramLists) yield {
        val concreteArgTypes = params.map(_.typeSignature.substituteTypes(classTypeParams, targetType.typeArgs))
        var index = 1
        for ((p, t) <- params.zip(concreteArgTypes)) yield {
          index += 1
          MethodArg(p, t)
        }
      }
    }

    def methodParmetersOf(targetType: ru.Type, method: MethodSymbol): Seq[RuntimeMethodParameter] = {
      val args = methodArgsOf(targetType, method).flatten
      val argTypes = args.map {x: MethodArg => resolveClass(x.tpe)}.toSeq
      val ref = MethodRef(resolveClass(targetType), method.name.decodedName.toString, argTypes, method.isConstructor)

      var index = 0
      val surfaceParams = args.map {arg =>
        val t = arg.name
        //accessor = { x : Any => x.asInstanceOf[${target.tpe}].${arg.paramName} }
        val expr = RuntimeMethodParameter(
          method = ref,
          index = index,
          name = arg.name,
          surface = arg.typeSurface
        )
        index += 1
        expr
      }
      // Using IndexedSeq is necessary for Serialization
      surfaceParams.toIndexedSeq
    }

    private def genericSurfaceWithConstructorFactory: SurfaceFactory = new SurfaceFactory with LogSupport {
      override def isDefinedAt(t: ru.Type): Boolean = {
        !isAbstract(t) && findPrimaryConstructorOf(t).exists(!_.paramLists.isEmpty)
      }
      override def apply(t: ru.Type): Surface = {
        val primaryConstructor = findPrimaryConstructorOf(t).get
        val typeArgs = typeArgsOf(t).map(surfaceOf(_)).toIndexedSeq
        new RuntimeGenericSurface(
          resolveClass(t),
          typeArgs,
          params = methodParmetersOf(t, primaryConstructor)
        )
      }
    }

    private def existentialTypeFactory: SurfaceFactory = {
      case t@ru.ExistentialType(quantified, underlying) =>
        surfaceOf(underlying)
    }

    private def genericSurfaceFactory: SurfaceFactory = {
      case t@TypeRef(prefix, symbol, args) if !args.isEmpty =>
        val typeArgs = typeArgsOf(t).map(surfaceOf(_)).toIndexedSeq
        new GenericSurface(resolveClass(t), typeArgs = typeArgs)
      case t@TypeRef(NoPrefix, symbol, args) if !t.typeSymbol.isClass =>
        wvlet.surface.ExistentialType
      case t =>
        new GenericSurface(resolveClass(t))
    }
  }

  class RuntimeGenericSurface(override val rawType: Class[_],
                              override val typeArgs: Seq[Surface] = Seq.empty,
                              override val params: Seq[Parameter] = Seq.empty)
    extends GenericSurface(rawType, typeArgs, params, None) {
    override val objectFactory: Option[ObjectFactory] = Some(
      new ObjectFactory {
        // Create instance with Reflection
        override def newInstance(args: Seq[Any]): Any = {
          val cc = rawType.getConstructors()(0)
          val obj = if (args.isEmpty) {
            cc.newInstance()
          }
          else {
            val a = args.map(_.asInstanceOf[AnyRef])
            cc.newInstance(a: _*)
          }
          obj.asInstanceOf[Any]
        }
      })
  }

}
