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

import scala.language.experimental.macros
import scala.reflect.macros.{blackbox => sm}

/**
  *
  */
object FrameMacros {

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
      typeArgsOf(t).map(toFrame(_)).head
    }

    private val toAlias: TypeMatcher = {
      case alias@TypeRef(prefix, symbol, args)
        if symbol.isType &&
          symbol.asType.isAliasType &&
          !belongsToScalaDefault(alias)
      =>
        val inner = toFrame(alias.dealias)
        val name = symbol.asType.name.decodedName.toString
        val fullName = s"${prefix.typeSymbol.fullName}.${name}"
        q"wvlet.frame.Alias(${name}, ${fullName}, $inner)"
    }

    private val toPrimitive: TypeMatcher = {
      case t if t =:= typeOf[Short] => q"wvlet.frame.Primitive.Short"
      case t if t =:= typeOf[Boolean] => q"wvlet.frame.Primitive.Boolean"
      case t if t =:= typeOf[Byte] => q"wvlet.frame.Primitive.Byte"
      case t if t =:= typeOf[Char] => q"wvlet.frame.Primitive.Char"
      case t if t =:= typeOf[Int] => q"wvlet.frame.Primitive.Int"
      case t if t =:= typeOf[Float] => q"wvlet.frame.Primitive.Float"
      case t if t =:= typeOf[Long] => q"wvlet.frame.Primitive.Long"
      case t if t =:= typeOf[Double] => q"wvlet.frame.Primitive.Double"
      case t if t =:= typeOf[String] => q"wvlet.frame.Primitive.String"
    }

    private val toArray: TypeMatcher = {
      case t if typeNameOf(t) == "scala.Array" =>
        q"wvlet.frame.ArrayFrame(classOf[$t], ${elementTypeOf(t)})"
    }


    private val toOption: TypeMatcher = {
      case t if typeNameOf(t) == "scala.Option" =>
        q"wvlet.frame.OptionFrame(classOf[$t], ${elementTypeOf(t)})"
    }

    private val toTuple : TypeMatcher = {
      case t if t <:< typeOf[Product] && t.typeSymbol.fullName.startsWith("scala.Tuple") =>
        val paramType = typeArgsOf(t).map(x => toFrame(x))
        q"new wvlet.frame.TupleFrame(classOf[$t], Seq(..$paramType))"
    }

    private val toJavaUtil : TypeMatcher = {
      case t if
      t =:= typeOf[java.io.File] ||
        t =:= typeOf[java.util.Date] ||
        t =:= typeOf[java.time.temporal.Temporal]
      =>
        q"wvlet.frame.ClassFrame(classOf[$t])"
    }

    private val toEnum : TypeMatcher = {
      case t if t.typeSymbol.isJavaEnum =>
        q"wvlet.frame.EnumFrame(classOf[$t])"
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

    private val toFrameWithParams: TypeMatcher = {
      case t@TypeRef(prefix, symbol, args) if !isAbstract(t) && findPrimaryConstructor(t).exists(!_.asMethod.paramLists.isEmpty) =>
        val primaryConstructor = findPrimaryConstructor(t).get
        val classTypeParams = t.typeSymbol.asClass.typeParams
        val params = primaryConstructor.asMethod.paramLists.flatten
        val concreteArgTypes = params.map(_.typeSignature.substituteTypes(classTypeParams, args))
        val frameParams = for ((p, t) <- params.zip(concreteArgTypes)) yield {
          val name = Literal(Constant(p.name.decodedName.toString))
          val frame = toFrame(t)
          val expr = q"wvlet.frame.Param($name, ${frame})"
          //println(s"p: ${showRaw(expr)}")
          //println(s"t: ${showRaw(t)}")
          expr
        }
        val typeArgs = typeArgsOf(t).map(toFrame(_))
        q"""new wvlet.frame.Frame {
             def rawType : Class[$t] = classOf[$t]
             override def typeArgs = Seq(..$typeArgs)
             override def params = Seq(..$frameParams)
            }"""
    }

    private val toExistentialType : TypeMatcher = {
      case t@ExistentialType(quantified, underlying) =>
        toFrame(underlying)
    }

    private val toGenericFrame: TypeMatcher = {
      case t@TypeRef(prefix, symbol, args) if !args.isEmpty =>
        val typeArgs = typeArgsOf(t).map(toFrame(_))
        q"new wvlet.frame.GenericFrame(classOf[$t], Seq(..$typeArgs))"
      case t@TypeRef(NoPrefix, symbol, args) if !t.typeSymbol.isClass =>
        //println(s"${showRaw(t)}")
        q"wvlet.frame.ExistentialType"
      case t =>
        q"wvlet.frame.ClassFrame(classOf[$t])"
    }

    private val matchers: TypeMatcher =
      toAlias orElse
        toPrimitive orElse
        toArray orElse
        toOption orElse
        toTuple orElse
        toJavaUtil orElse
        toEnum orElse
        toFrameWithParams orElse
        toExistentialType orElse
        toGenericFrame

    def toFrame(t: c.Type): c.Tree = {
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
        val frame = matchers(t)
        memo += (t -> frame)
        val fullName = extractFullName(t)
        q"wvlet.frame.Frame.frameCache.getOrElseUpdate(${fullName}, ${frame})"
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

    def createFrame(typeEv: c.Type): c.Tree = {
      toFrame(typeEv)
    }
  }

  def of[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    val typeEv = implicitly[c.WeakTypeTag[A]].tpe
    new Helper[c.type](c).createFrame(typeEv)
  }
}
