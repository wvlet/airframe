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

import scala.language.experimental.macros
import scala.reflect.macros.{blackbox => sm}

/**
  *
  */
object SurfaceMacros {

  class Helper[C <: sm.Context](val c: C) {

    import c.universe._

    val seen = scala.collection.mutable.Set[Type]()
    val memo = scala.collection.mutable.Map[Type, c.Tree]()

    type TypeMatcher = PartialFunction[c.Type, c.Tree]

    private def typeArgsOf(t: c.Type): List[c.Type] = t match {
      case TypeRef(prefix, symbol, args) =>
        args
      case other =>
        List.empty
    }

    private def typeNameOf(t: c.Type): String = {
      t.dealias.typeSymbol.fullName
    }

    private def elementTypeOf(t: c.Type): c.Tree = {
      typeArgsOf(t).map(toSurface(_)).head
    }

    private val toAlias: TypeMatcher = {
      case alias@TypeRef(prefix, symbol, args)
        if symbol.isType &&
          symbol.asType.isAliasType &&
          !belongsToScalaDefault(alias)
      =>
        val inner = toSurface(alias.dealias)
        val name = symbol.asType.name.decodedName.toString
        val fullName = s"${prefix.typeSymbol.fullName}.${name}"
        q"wvlet.surface.Alias(${name}, ${fullName}, $inner)"
    }

    private val toPrimitive: TypeMatcher = {
      case t if t =:= typeOf[Short] => q"wvlet.surface.Primitive.Short"
      case t if t =:= typeOf[Boolean] => q"wvlet.surface.Primitive.Boolean"
      case t if t =:= typeOf[Byte] => q"wvlet.surface.Primitive.Byte"
      case t if t =:= typeOf[Char] => q"wvlet.surface.Primitive.Char"
      case t if t =:= typeOf[Int] => q"wvlet.surface.Primitive.Int"
      case t if t =:= typeOf[Float] => q"wvlet.surface.Primitive.Float"
      case t if t =:= typeOf[Long] => q"wvlet.surface.Primitive.Long"
      case t if t =:= typeOf[Double] => q"wvlet.surface.Primitive.Double"
      case t if t =:= typeOf[String] => q"wvlet.surface.Primitive.String"
    }

    private val toArray: TypeMatcher = {
      case t if typeNameOf(t) == "scala.Array" =>
        q"wvlet.surface.ArraySurface(classOf[$t], ${elementTypeOf(t)})"
    }


    private val toOption: TypeMatcher = {
      case t if typeNameOf(t) == "scala.Option" =>
        q"wvlet.surface.OptionSurface(classOf[$t], ${elementTypeOf(t)})"
    }

    private val toTuple : TypeMatcher = {
      case t if t <:< typeOf[Product] && t.typeSymbol.fullName.startsWith("scala.Tuple") =>
        val paramType = typeArgsOf(t).map(x => toSurface(x))
        q"new wvlet.surface.TupleSurface(classOf[$t], Seq(..$paramType))"
    }

    private val toJavaUtil : TypeMatcher = {
      case t if
      t =:= typeOf[java.io.File] ||
        t =:= typeOf[java.util.Date] ||
        t =:= typeOf[java.time.temporal.Temporal]
      =>
        q"new wvlet.surface.GenericSurface(classOf[$t])"
    }

    private val toEnum : TypeMatcher = {
      case t if t.typeSymbol.isJavaEnum =>
        q"wvlet.surface.EnumSurface(classOf[$t])"
    }

    private val scalaDefaultPackages = Seq("scala.", "scala.Predef.", "scala.util.")
    private def belongsToScalaDefault(t: c.Type) = {
      t match {
        case TypeRef(prefix, _, _) =>
          scalaDefaultPackages.exists(p => prefix.dealias.typeSymbol.fullName.startsWith(p))
        case _ => false
      }
    }


    private def findPrimaryConstructor(t: c.Type) = {
      t.members.find(x => x.isMethod && x.asMethod.isPrimaryConstructor)
    }

    def hasAbstractMethods(t: c.Type): Boolean = t.members.exists(x =>
      x.isMethod && x.isAbstract && !x.isAbstractOverride
    )

    private def isAbstract(t: c.Type): Boolean = {
      t.typeSymbol.isAbstract && hasAbstractMethods(t)
    }

    private val toSurfaceWithParams: TypeMatcher = {
      case t@TypeRef(prefix, symbol, args) if !isAbstract(t) && findPrimaryConstructor(t).exists(!_.asMethod.paramLists.isEmpty) =>
        val primaryConstructor = findPrimaryConstructor(t).get
        val classTypeParams = t.typeSymbol.asClass.typeParams
        val params = primaryConstructor.asMethod.paramLists.flatten
        val concreteArgTypes = params.map(_.typeSignature.substituteTypes(classTypeParams, args))
        val surfaceParams = for ((p, t) <- params.zip(concreteArgTypes)) yield {
          val name = Literal(Constant(p.name.decodedName.toString))
          val surface = toSurface(t)
          val expr = q"wvlet.surface.Param($name, ${surface})"
          //println(s"p: ${showRaw(expr)}")
          //println(s"t: ${showRaw(t)}")
          expr
        }
        val typeArgs = typeArgsOf(t).map(toSurface(_))
        q"new wvlet.surface.GenericSurface(classOf[$t], Seq(..$typeArgs), Seq(..$surfaceParams))"
    }

    private val toExistentialType : TypeMatcher = {
      case t@ExistentialType(quantified, underlying) =>
        toSurface(underlying)
    }

    private val toGenericSurface: TypeMatcher = {
      case t@TypeRef(prefix, symbol, args) if !args.isEmpty =>
        val typeArgs = typeArgsOf(t).map(toSurface(_))
        q"new wvlet.surface.GenericSurface(classOf[$t], Seq(..$typeArgs))"
      case t@TypeRef(NoPrefix, symbol, args) if !t.typeSymbol.isClass =>
        //println(s"${showRaw(t)}")
        q"wvlet.surface.ExistentialType"
      case t =>
        q"new wvlet.surface.GenericSurface(classOf[$t])"
    }

    private val matchers: TypeMatcher =
      toAlias orElse
        toPrimitive orElse
        toArray orElse
        toOption orElse
        toTuple orElse
        toJavaUtil orElse
        toEnum orElse
        toSurfaceWithParams orElse
        toExistentialType orElse
        toGenericSurface

    def toSurface(t: c.Type): c.Tree = {
      if (seen.contains(t)) {
        if (memo.contains(t)) {
          memo(t)
        }
        else {
          c.abort(c.enclosingPosition, s"recursive type: ${t.typeSymbol.fullName}")
        }
      }
      else {
        seen += t
        //println(s"fullName: ${t.dealias.typeSymbol.fullName}")
        val surface = matchers(t)
        memo += (t -> surface)
        val fullName = extractFullName(t)
        q"wvlet.surface.Surface.surfaceCache.getOrElseUpdate(${fullName}, ${surface})"
      }
    }

    def extractFullName(typeEv: c.Type): String = {
      typeEv match {
        case TypeRef(prefix, typeSymbol, args) =>
          if (args.isEmpty) {
            typeSymbol.fullName
          }
          else {
            val typeArgs = args.map(extractFullName(_)).mkString(",")
            s"${typeSymbol.fullName}[${typeArgs}]"
          }
        case other =>
          typeEv.typeSymbol.fullName
      }
    }

    def createSurface(typeEv: c.Type): c.Tree = {
      toSurface(typeEv)
    }
  }

  def of[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    val typeEv = implicitly[c.WeakTypeTag[A]].tpe
    new Helper[c.type](c).createSurface(typeEv)
  }
}
