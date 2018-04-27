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
private[surface] object SurfaceMacros {

  def of[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    val targetType = implicitly[c.WeakTypeTag[A]].tpe
    new SurfaceGenerator[c.type](c).createSurfaceOf(targetType)
  }

  def methodsOf[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    val targetType = implicitly[c.WeakTypeTag[A]].tpe
    new SurfaceGenerator[c.type](c).createMethodSurfaceOf(targetType)
  }

  private[surface] class SurfaceGenerator[C <: sm.Context](val c: C) {

    import c.universe._

    private val seen       = scala.collection.mutable.Set[Type]()
    private val memo       = scala.collection.mutable.Map[Type, c.Tree]()
    private val methodSeen = scala.collection.mutable.Set[Type]()
    private val methodMemo = scala.collection.mutable.Map[Type, c.Tree]()

    type SurfaceFactory = PartialFunction[c.Type, c.Tree]

    def createSurfaceOf(targetType: c.Type): c.Tree = {
      surfaceOf(targetType)
    }

    def localMethodsOf(t: c.Type): Iterable[MethodSymbol] = {
      t.members.filter(x => x.isMethod && !x.isConstructor && !x.isImplementationArtifact).map(_.asMethod).filter(isTargetMethod(_, t))
    }

    def createMethodSurfaceOf(targetType: c.Type): c.Tree = {
      if (methodMemo.contains(targetType)) {
        methodMemo(targetType)
      } else {
        if (methodSeen.contains(targetType)) {
          c.abort(c.enclosingPosition, s"recursive type in method: ${targetType.typeSymbol.fullName}")
        }
        methodSeen += targetType
        val result = targetType match {
          case t @ TypeRef(prefix, typeSymbol, typeArgs) =>
            val list = for (m <- localMethodsOf(t.dealias)) yield {
              val mod   = modifierBitMaskOf(m)
              val owner = surfaceOf(t)
              val name  = m.name.decodedName.toString
              val ret   = surfaceOf(m.returnType)
              val args  = methodParmetersOf(m.owner.typeSignature, m)
              q"wvlet.surface.ClassMethodSurface(${mod}, ${owner}, ${name}, ${ret}, ${args}.toIndexedSeq)"
            }
            q"IndexedSeq(..$list)"
          case _ =>
            q"Seq.empty"
        }

        val fullName = fullTypeNameOf(targetType.dealias)
        val expr     = q"wvlet.surface.methodSurfaceCache.getOrElseUpdate(${fullName}, ${result})"
        methodMemo += targetType -> expr
        expr
      }
    }

    private def isEnum(t: c.Type): Boolean = {
      t.baseClasses.exists(x => x.isJava && x.isType && x.asType.name.decodedName.toString.startsWith("java.lang.Enum"))
    }

    private def typeArgsOf(t: c.Type): List[c.Type] = t match {
      case TypeRef(prefix, symbol, args) =>
        args
      case ExistentialType(quantified, underlying) =>
        typeArgsOf(underlying)
      case other =>
        List.empty
    }

    private def typeNameOf(t: c.Type): String = {
      t.dealias.typeSymbol.fullName
    }

    private def elementTypeOf(t: c.Type): c.Tree = {
      typeArgsOf(t).map(surfaceOf(_)).head
    }

    private val taggedTypeFactory: SurfaceFactory = {
      case t if t.typeArgs.length == 2 && typeNameOf(t).startsWith("wvlet.surface.tag.") =>
        val typeArgs = t.typeArgs
        q"wvlet.surface.TaggedSurface(${surfaceOf(typeArgs(0))}, ${surfaceOf(typeArgs(1))})"
    }

    private val aliasFactory: SurfaceFactory = {
      case alias @ TypeRef(prefix, symbol, args)
          if symbol.isType &&
            symbol.asType.isAliasType &&
            !belongsToScalaDefault(alias) =>
        val inner    = surfaceOf(alias.dealias)
        val name     = symbol.asType.name.decodedName.toString
        val fullName = s"${prefix.typeSymbol.fullName}.${name}"
        q"wvlet.surface.Alias(${name}, ${fullName}, $inner)"
    }

    private val primitiveFactory: SurfaceFactory = {
      case t if t == typeOf[Short]            => q"wvlet.surface.Primitive.Short"
      case t if t == typeOf[Boolean]          => q"wvlet.surface.Primitive.Boolean"
      case t if t == typeOf[Byte]             => q"wvlet.surface.Primitive.Byte"
      case t if t == typeOf[Char]             => q"wvlet.surface.Primitive.Char"
      case t if t == typeOf[Int]              => q"wvlet.surface.Primitive.Int"
      case t if t == typeOf[Float]            => q"wvlet.surface.Primitive.Float"
      case t if t == typeOf[Long]             => q"wvlet.surface.Primitive.Long"
      case t if t == typeOf[Double]           => q"wvlet.surface.Primitive.Double"
      case t if t == typeOf[String]           => q"wvlet.surface.Primitive.String"
      case t if t == typeOf[java.lang.String] => q"wvlet.surface.Primitive.String"
      case t if t == typeOf[Unit]             => q"wvlet.surface.Primitive.Unit"
    }

    private val arrayFactory: SurfaceFactory = {
      case t if typeNameOf(t) == "scala.Array" =>
        q"wvlet.surface.ArraySurface(classOf[$t], ${elementTypeOf(t)})"
    }

    private val optionFactory: SurfaceFactory = {
      case t if typeNameOf(t) == "scala.Option" =>
        q"wvlet.surface.OptionSurface(classOf[$t], ${elementTypeOf(t)})"
    }

    private val tupleFactory: SurfaceFactory = {
      case t if t <:< typeOf[Product] && t.typeSymbol.fullName.startsWith("scala.Tuple") =>
        val paramType = typeArgsOf(t).map(x => surfaceOf(x))
        q"new wvlet.surface.TupleSurface(classOf[$t], IndexedSeq(..$paramType))"
    }

    private val javaUtilFactory: SurfaceFactory = {
      case t
          if t =:= typeOf[java.io.File] ||
            t =:= typeOf[java.util.Date] ||
            t =:= typeOf[java.time.temporal.Temporal] =>
        q"new wvlet.surface.GenericSurface(classOf[$t])"
    }

    private val enumFactory: SurfaceFactory = {
      case t if isEnum(t) =>
        q"wvlet.surface.EnumSurface(classOf[$t])"
    }

    private def belongsToScalaDefault(t: c.Type) = {
      t match {
        case TypeRef(prefix, _, _) =>
          val scalaDefaultPackages = Seq("scala.", "scala.Predef.", "scala.util.")
          scalaDefaultPackages.exists(p => prefix.dealias.typeSymbol.fullName.startsWith(p))
        case _ => false
      }
    }

    private def isPhantomConstructor(constructor: Symbol): Boolean =
      constructor.asMethod.fullName.endsWith("$init$")

    def publicConstructorsOf(t: c.Type): Iterable[MethodSymbol] = {
      t.members.filter(m => m.isMethod && m.asMethod.isConstructor && m.isPublic).filterNot(isPhantomConstructor).map(_.asMethod)
    }

    def findPrimaryConstructorOf(t: c.Type): Option[MethodSymbol] = {
      publicConstructorsOf(t).find(x => x.isPrimaryConstructor)
    }

    def hasAbstractMethods(t: c.Type): Boolean = t.members.exists(x => x.isMethod && x.isAbstract && !x.isAbstractOverride)

    private def isAbstract(t: c.Type): Boolean = {
      t.typeSymbol.isAbstract && hasAbstractMethods(t)
    }

    case class MethodArg(paramName: Symbol, tpe: c.Type, defaultValue: Option[c.Tree]) {
      def name: Literal         = Literal(Constant(paramName.name.decodedName.toString))
      private def paramNameTerm = TermName(paramName.name.decodedName.toString)
      def typeSurface: c.Tree   = surfaceOf(tpe)

      def accessor(t: c.Type): c.Tree = {
        if (t.typeSymbol.isAbstract && !(t <:< typeOf[AnyVal])) {
          q"None"
        } else {
          t.typeArgs.size match {
            // TODO We need to expand Select(Ident(x.y.z....), TermName("a")) =>
            // Select(Select(Select(Ident(TermName("x")), TermName("y")), ....
            case 0     => q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}].${paramNameTerm}})"
            case 1     => q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_]].${paramNameTerm}})"
            case 2     => q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_, _]].${paramNameTerm}})"
            case 3     => q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_, _, _]].${paramNameTerm}})"
            case 4     => q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_, _, _, _]].${paramNameTerm}})"
            case 5     => q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_, _, _, _, _]].${paramNameTerm}})"
            case 6     => q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_, _, _, _, _, _]].${paramNameTerm}})"
            case 7     => q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_, _, _, _, _, _, _]].${paramNameTerm}})"
            case 8     => q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_, _, _, _, _, _, _, _]].${paramNameTerm}})"
            case other => q"None"
          }
        }
      }
    }

    private def findMethod(m: Type, name: String): Option[MethodSymbol] = {
      m.member(TermName(name)) match {
        case NoSymbol => None
        case other    => Some(other.asMethod)
      }
    }

    /**
      * Returns the list of the method argument lists (supporting multiple param block methods)
      */
    def methodArgsOf(targetType: c.Type, constructor: MethodSymbol): List[List[MethodArg]] = {
      val classTypeParams = targetType.typeSymbol.asClass.typeParams

      val companion = targetType.companion match {
        case NoType => None
        case comp   => Some(comp)
      }

      val ret = for (params <- constructor.paramLists) yield {
        val concreteArgTypes = params.map(_.typeSignature.substituteTypes(classTypeParams, targetType.typeArgs))
        var index            = 1
        for ((p, t) <- params.zip(concreteArgTypes)) yield {

          // Find the default argument of the method parameter
          val defaultValue =
            companion.flatMap { x =>
              // Find default value getter from the companion class
              val defaultValueGetter =
                findMethod(x, "apply$default$" + index).orElse(findMethod(x, "$lessinit$greater$default$" + index))
              defaultValueGetter.map { g =>
                q"${g}"
              }
            }

          index += 1
          MethodArg(p, t, defaultValue)
        }
      }
      ret
    }

    def toClassOf(t: c.Type): c.Tree = {
      if (t.typeSymbol.isAbstract && !(t <:< typeOf[AnyVal])) {
        q"classOf[AnyRef]"
      } else {
        t.typeArgs.size match {
          case 0     => q"classOf[${t.typeSymbol}]"
          case 1     => q"classOf[${t.typeSymbol}[_]]"
          case 2     => q"classOf[${t.typeSymbol}[_,_]]"
          case 3     => q"classOf[${t.typeSymbol}[_,_,_]]"
          case 4     => q"classOf[${t.typeSymbol}[_,_,_,_]]"
          case 5     => q"classOf[${t.typeSymbol}[_,_,_,_,_]]"
          case 6     => q"classOf[${t.typeSymbol}[_,_,_,_,_,_]]"
          case 7     => q"classOf[${t.typeSymbol}[_,_,_,_,_,_,_]]"
          case 8     => q"classOf[${t.typeSymbol}[_,_,_,_,_,_,_,_]]"
          case other => q"classOf[AnyRef]"
        }
      }
    }

    def methodParmetersOf(targetType: c.Type, method: MethodSymbol): c.Tree = {
      val args = methodArgsOf(targetType, method).flatten
      val argTypes = args.map { x: MethodArg =>
        toClassOf(x.tpe)
      }
      val ref = q"wvlet.surface.MethodRef(${toClassOf(targetType)}, ${method.name.decodedName.toString}, Seq(..$argTypes), ${method.isConstructor})"

      var index = 0
      val surfaceParams = args.map { arg =>
        val t = arg.name
        val defaultValue = arg.defaultValue match {
          case Some(x) => q"Some(${x})"
          case other   => q"None"
        }

        val expr =
          q"""wvlet.surface.StdMethodParameter(
            method = ${ref},
            index = ${index},
            name=${arg.name},
            surface = ${arg.typeSurface},
            defaultValue = ${defaultValue},
            accessor = ${arg.accessor(targetType)}
          )
          """
        index += 1
        expr
      }
      // Using IndexedSeq is necessary for Serialization
      q"IndexedSeq(..${surfaceParams})"
    }

    def createObjectFactoryOf(targetType: c.Type): Option[c.Tree] = {
      if (isAbstract(targetType)) {
        None
      } else {
        findPrimaryConstructorOf(targetType).map { primaryConstructor =>
          val argsList   = methodArgsOf(targetType, primaryConstructor)
          var index: Int = 0
          val argExtractor: List[List[c.Tree]] =
            for (arg <- argsList) yield {
              for (a <- arg) yield {
                val param = Apply(Ident(TermName("args")), List(Literal(Constant(index))))
                index += 1
                // TODO natural type conversion (e.g., Int -> Long, etc.)
                q"${param}.asInstanceOf[${a.tpe}]"
              }
            }

          // Create a constructor call
          val constructor: c.Tree =
            argExtractor.foldLeft[c.Tree](Select(New(Ident(targetType.dealias.typeSymbol)), termNames.CONSTRUCTOR))((x, arg) => Apply(x, arg))

          // TODO: Support companion object call for instanciating the object
          val expr =
            q"""new wvlet.surface.ObjectFactory {
            def newInstance(args:Seq[Any]) : ${targetType} = { $constructor }
          }
          """
          expr
        }
      }
    }

    private val genericSurfaceWithConstructorFactory: SurfaceFactory = new SurfaceFactory {
      override def isDefinedAt(t: c.Type): Boolean = {
        !isAbstract(t) && findPrimaryConstructorOf(t).exists(!_.paramLists.isEmpty)
      }
      override def apply(t: c.Type): c.Tree = {
        val primaryConstructor = findPrimaryConstructorOf(t).get
        val typeArgs           = typeArgsOf(t).map(surfaceOf(_))
        val factory = createObjectFactoryOf(t) match {
          case Some(x) => q"Some($x)"
          case None    => q"None"
        }
        q"""
          new wvlet.surface.GenericSurface(
            classOf[$t],
            IndexedSeq(..$typeArgs),
            params = ${methodParmetersOf(t, primaryConstructor)},
            objectFactory=${factory}
        )"""
      }
    }

    private val existentialTypeFactory: SurfaceFactory = {
      case t @ ExistentialType(quantified, underlying) =>
        surfaceOf(underlying)
    }

    private val genericSurfaceFactory: SurfaceFactory = {
      case t @ TypeRef(prefix, symbol, args) if !args.isEmpty =>
        val typeArgs = typeArgsOf(t).map(surfaceOf(_))
        q"new wvlet.surface.GenericSurface(classOf[$t], typeArgs = IndexedSeq(..$typeArgs))"
      case t @ TypeRef(NoPrefix, symbol, args) if !t.typeSymbol.isClass =>
        q"wvlet.surface.ExistentialType"
      case t =>
        val expr = q"new wvlet.surface.GenericSurface(classOf[$t])"
        expr
    }

    private val surfaceFactories: SurfaceFactory =
      primitiveFactory orElse
        taggedTypeFactory orElse
        aliasFactory orElse
        arrayFactory orElse
        optionFactory orElse
        tupleFactory orElse
        javaUtilFactory orElse
        enumFactory orElse
        genericSurfaceWithConstructorFactory orElse
        existentialTypeFactory orElse
        genericSurfaceFactory

    def surfaceOf(t: c.Type): c.Tree = {
      if (seen.contains(t)) {
        if (memo.contains(t)) {
          memo(t)
        } else {
          val typeArgs = typeArgsOf(t).map(surfaceOf(_))
          q"wvlet.surface.LazySurface(classOf[${t}], ${fullTypeNameOf(t)}, IndexedSeq(..$typeArgs))"
        }
      } else {
        seen += t
        // We don't need to cache primitive types
        val surfaceGenerator =
          surfaceFactories andThen { tree =>
            // cache the generated Surface instance
            q"wvlet.surface.surfaceCache.getOrElseUpdate(${fullTypeNameOf(t)}, ${tree})"
          }
        val surface = surfaceGenerator(t)
        memo += (t -> surface)
        surface
      }
    }

    /**
      * Get a string representation of the type identifier
      *
      * @param typeEv
      * @return
      */
    private def fullTypeNameOf(typeEv: c.Type): String = {
      typeEv match {
        case TypeRef(prefix, typeSymbol, args) if args.isEmpty => typeSymbol.fullName
        case TypeRef(prefix, typeSymbol, args) if !args.isEmpty =>
          val typeArgs = args.map(fullTypeNameOf(_)).mkString(",")
          s"${typeSymbol.fullName}[${typeArgs}]"
        case _ => typeEv.typeSymbol.fullName
      }
    }

    private def isOwnedByTargetClass(m: MethodSymbol, t: c.Type): Boolean = {
      m.owner == t.typeSymbol
    }

    private def isTargetMethod(m: MethodSymbol, target: c.Type): Boolean = {
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

    def modifierBitMaskOf(m: MethodSymbol): Int = {
      var mod = 0
      if (m.isPublic) {
        mod |= MethodModifier.PUBLIC
      }
      if (m.isPrivate) {
        mod |= MethodModifier.PRIVATE
      }
      if (m.isProtected) {
        mod |= MethodModifier.PROTECTED
      }
      if (m.isStatic) {
        mod |= MethodModifier.STATIC
      }
      if (m.isFinal) {
        mod |= MethodModifier.FINAL
      }
      if (m.isAbstract) {
        mod |= MethodModifier.ABSTRACT
      }
      mod
    }
  }
}
