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

    private def isEnum(t: c.Type): Boolean = {
      t.baseClasses.exists(x =>
        x.isJava && x.isType && x.asType.name.decodedName.toString.startsWith("java.lang.Enum")
      )
    }

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

    private val toTuple: TypeMatcher = {
      case t if t <:< typeOf[Product] && t.typeSymbol.fullName.startsWith("scala.Tuple") =>
        val paramType = typeArgsOf(t).map(x => toSurface(x))
        q"new wvlet.surface.TupleSurface(classOf[$t], Seq(..$paramType))"
    }

    private val toJavaUtil: TypeMatcher = {
      case t if
      t =:= typeOf[java.io.File] ||
        t =:= typeOf[java.util.Date] ||
        t =:= typeOf[java.time.temporal.Temporal]
      =>
        q"new wvlet.surface.GenericSurface(classOf[$t])"
    }

    private val toEnum: TypeMatcher = {
      case t if isEnum(t) =>
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

    private def getArgList(owner: c.Type, typeArgs: List[c.Type], m: MethodSymbol): c.Tree = {
      val classTypeParams = owner.typeSymbol.asClass.typeParams
      val params = m.paramLists.flatten
      val concreteArgTypes = params.map(_.typeSignature.substituteTypes(classTypeParams, typeArgs))
      val surfaceParams = for ((p, t) <- params.zip(concreteArgTypes)) yield {
        val name = Literal(Constant(p.name.decodedName.toString))
        val surface = toSurface(t)
        val expr = q"wvlet.surface.Param($name, ${surface})"
        expr
      }
      q"Seq(..${surfaceParams})"
    }

    private val toSurfaceWithParams: TypeMatcher = {
      case t@TypeRef(prefix, symbol, args) if !isAbstract(t) && findPrimaryConstructor(t).exists(!_.asMethod.paramLists.isEmpty) =>
        val primaryConstructor = findPrimaryConstructor(t).get
        val surfaceParams = getArgList(t, args, primaryConstructor.asMethod)
        val typeArgs = typeArgsOf(t).map(toSurface(_))
        q"new wvlet.surface.GenericSurface(classOf[$t], Seq(..$typeArgs), $surfaceParams)"
    }

    private val toExistentialType: TypeMatcher = {
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
        //surface
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

    def isOwnedByTargetClass(m: MethodSymbol, t: c.Type): Boolean = {
      m.owner == t.typeSymbol
    }

    def isTargetMethod(m: MethodSymbol, target: c.Type): Boolean = {
      // synthetic is used for functions returning default values of method arguments (e.g., ping$default$1)
      val methodName = m.name.decodedName.toString
      m.isMethod &&
        !m.isImplicit &&
        !m.isSynthetic &&
        !m.isAccessor &&
        !methodName.startsWith("$") &&
        methodName != "<init>" &&
        isOwnedByTargetClass(m, target)
    }

    def getModifier(m:MethodSymbol) : Int = {
      var mod = 0
      if(m.isPublic) {
        mod |= 0x1
      }
      if(m.isPrivate) {
        mod |= 0x2
      }
      if(m.isProtected) {
        mod |= 0x4
      }
      if(m.isStatic) {
        mod |= 0x8
      }
      if(m.isFinal) {
        mod |= 0x10
      }
      if(m.isAbstract) {
        mod |= 0x400
      }
      mod
    }

    def annotationsOf(m:MethodSymbol) : c.Tree = {
      val annots = for(a <- m.annotations) yield {
        a.tree
      }
      q"Seq[java.lang.annotation.Annotation](..$annots)"
    }

    def createMethodSurface(typeEv: c.Type): c.Tree = {
      val result = typeEv match {
        case t@TypeRef(prefix, typeSymbol, typeArgs) =>
          val list = for {
            m <- typeEv
                 .members
                 .filter(x => x.isMethod)
                 .map(_.asMethod)
                 .filterNot(_.isConstructor)
                 .filter(x => isTargetMethod(x, typeEv))
          } yield {
            val name = m.name.decodedName.toString
            val owner = toSurface(t)
            val ret = toSurface(m.returnType)
            val args = getArgList(m.owner.typeSignature, typeArgs, m)
            val mod = getModifier(m)
            // TODO how to pass annotation info to runtime
            //val annot = annotationsOf(m)
            //println(s"annot: ${show(annot)}")
            val expr = q"wvlet.surface.ClassMethodSurface(${mod}, ${owner}, ${name}, ${ret}, ${args}.toIndexedSeq)"
            //println(s"expr: ${show(expr)}")
            expr
          }
          q"Seq(..$list)"
        case _ =>
          q"Seq()"
      }
      val fullName = extractFullName(typeEv)
      q"wvlet.surface.Surface.methodSurfaceCache.getOrElseUpdate(${fullName}, ${result})"
    }
  }



  def of[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    val typeEv = implicitly[c.WeakTypeTag[A]].tpe
    new Helper[c.type](c).createSurface(typeEv)
  }

  def methodsOf[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    val typeEv = implicitly[c.WeakTypeTag[A]].tpe
    new Helper[c.type](c).createMethodSurface(typeEv)
  }
}
