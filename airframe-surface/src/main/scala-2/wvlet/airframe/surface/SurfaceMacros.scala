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

import scala.language.experimental.macros
import scala.reflect.macros.{blackbox => sm}

/**
  * Surface generation Macros for Scala.js.
  *
  * This code needs to be almost the same with ReflectSurfaceFactory
  */
private[surface] object SurfaceMacros {
  def surfaceOf[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val targetType = implicitly[c.WeakTypeTag[A]].tpe
    val t          = targetType.typeSymbol
    if (t.isStatic) {
      q"wvlet.airframe.surface.SurfaceFactory.of[${targetType}]"
    } else {
      // If t is non-static class (e.g., this.A), we need to pass its outer context instance (as this)
      val owner = t.owner
      owner match {
        case o if o.isClass && o != NoSymbol && !o.isAbstract =>
          q"wvlet.airframe.surface.SurfaceFactory.localSurfaceOf[${targetType}](${t.owner}.this)"
        case _ =>
          q"wvlet.airframe.surface.SurfaceFactory.localSurfaceOf[${targetType}](this)"
      }
    }
  }

  def methodSurfaceOf[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val targetType = implicitly[c.WeakTypeTag[A]].tpe
    q"wvlet.airframe.surface.SurfaceFactory.methodsOf[${targetType}]"
  }

  def of[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    val targetType = implicitly[c.WeakTypeTag[A]].tpe
    new SurfaceGenerator[c.type](c).createSurfaceOf(targetType)
  }

  def localSurfaceOf[A: c.WeakTypeTag](c: sm.Context)(context: c.Tree): c.Tree = {
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

    private def allMethodsOf(t: c.Type): Iterable[MethodSymbol] = {
      // Sort the members in the source code order
      t.members.sorted
        .filter(x =>
          nonObject(x.owner) &&
            x.isMethod &&
            x.isPublic &&
            !x.isConstructor &&
            !x.isImplementationArtifact &&
            !x.isMacro &&
            !x.isImplicit &&
            // synthetic is used for functions returning default values of method arguments (e.g., ping$default$1)
            !x.isSynthetic
        )
        .map(_.asMethod)
        .filter { x =>
          val name = x.name.decodedName.toString
          !x.isAccessor &&
          !name.startsWith("$") &&
          name != "<init>"
        }
    }

    def localMethodsOf(t: c.Type): Iterable[MethodSymbol] = {
      allMethodsOf(t).filter(m => isOwnedByTargetClass(m, t))
    }

    private def nonObject(x: c.Symbol): Boolean = {
      !x.isImplementationArtifact &&
      !x.isSynthetic &&
      // !x.isAbstract &&
      x.fullName != "scala.Any" &&
      x.fullName != "java.lang.Object"
    }

    private def isOwnedByTargetClass(m: MethodSymbol, t: c.Type): Boolean = {
      m.owner == t.typeSymbol || t.baseClasses
        .filter(nonObject)
        .exists(_ == m.owner)
    }

    private def createMethodCaller(t: c.Type, m: MethodSymbol, methodArgs: Seq[MethodArg]): c.Tree = {
      val methodName = TermName(m.name.encodedName.toString)
      if (m.isPublic) {
        if (methodArgs.size == 0) {
          q"""
         Some({ (x: Any, args: Seq[Any]) =>  x.asInstanceOf[${t}].${methodName} })
            """
        } else {
          val argList = methodArgs.zipWithIndex.map { case (x, i) =>
            q"args(${i}).asInstanceOf[${x.tpe}]"
          }
          q"""
         Some({ (x: Any, args: Seq[Any]) =>  x.asInstanceOf[${t}].${methodName}(..${argList}) })
            """
        }
      } else {
        // Fallback for an unknown error
        q"None"
      }
    }

    def createMethodSurfaceOf(targetType: c.Type): c.Tree = {
      if (methodMemo.contains(targetType)) {
        methodMemo(targetType)
      } else {
        if (methodSeen.contains(targetType)) {
          c.abort(c.enclosingPosition, s"recursive type in method: ${targetType.typeSymbol.fullName}")
        }
        methodSeen += targetType
        val localMethods = targetType match {
          case t @ TypeRef(prefix, typeSymbol, typeArgs) =>
            localMethodsOf(t.dealias).toSeq.distinct
          case t @ RefinedType(List(_, baseType), decls) =>
            (localMethodsOf(baseType) ++ localMethodsOf(t)).toSeq.distinct
          case _ => Seq.empty
        }

        val result = {
          val lst = IndexedSeq.newBuilder[c.Tree]
          for (m <- localMethods) {
            try {
              val mod        = modifierBitMaskOf(m)
              val owner      = surfaceOf(targetType)
              val name       = m.name.decodedName.toString
              val ret        = surfaceOf(m.returnType)
              val methodArgs = methodArgsOf(targetType, m).flatten
              val args =
                methodParametersOf(m.owner.typeSignature, m, methodArgs)
              // Generate code for supporting ClassMethodSurface.call(instance, args)
              val methodCaller = createMethodCaller(targetType, m, methodArgs)
              lst += q"wvlet.airframe.surface.ClassMethodSurface(${mod}, ${owner}, ${name}, ${ret}, ${args}.toIndexedSeq, ${methodCaller})"
            } catch {
              case e: Throwable =>
                c.warning(c.enclosingPosition, s"${e.getMessage}")
            }
          }
          q"IndexedSeq(..${lst.result()})"
        }

        val fullName = fullTypeNameOf(targetType.dealias)
        val expr =
          q"wvlet.airframe.surface.methodSurfaceCache.getOrElseUpdate(${fullName}, ${result})"
        methodMemo += targetType -> expr
        expr
      }
    }

    private def isEnum(t: c.Type): Boolean = {
      t.baseClasses.exists(x =>
        x.isJava && x.isType && x.asType.name.decodedName.toString
          .startsWith("java.lang.Enum")
      )
    }

    private def typeArgsOf(t: c.Type): List[c.Type] =
      t match {
        case TypeRef(prefix, symbol, args) =>
          args
        case ExistentialType(quantified, underlying) =>
          typeArgsOf(underlying)
        case other =>
          List.empty
      }

    private def sanitizeTypeName(s: String): String = {
      s.stripSuffix("$").replaceAll("\\.package\\$", ".").replaceAll("\\$+", ".")
    }

    private def typeNameOf(t: c.Type): String = {
      sanitizeTypeName(t.dealias.typeSymbol.fullName)
    }

    private def elementTypeOf(t: c.Type): c.Tree = {
      typeArgsOf(t).map(surfaceOf(_)).head
    }

    private def isTaggedType(t: c.Type): Boolean = {
      typeNameOf(t).startsWith("wvlet.airframe.surface.tag.")
    }

    private val taggedTypeFactory: SurfaceFactory = {
      case t if t.typeArgs.length == 2 && isTaggedType(t) =>
        val typeArgs = t.typeArgs
        q"wvlet.airframe.surface.TaggedSurface(${surfaceOf(typeArgs(0))}, ${surfaceOf(typeArgs(1))})"
    }

    private val aliasFactory: SurfaceFactory = {
      case alias @ TypeRef(prefix, symbol, args)
          if symbol.isType &&
            symbol.asType.isAliasType &&
            !belongsToScalaDefault(alias) =>
        val dealiased = alias.dealias
        val inner = if (alias != dealiased) {
          surfaceOf(dealiased)
        } else {
          // When higher kind types are aliased (e.g., type M[A] = Future[A]),
          // alias.dealias will not return the aliased type (Future[A]),
          // So we need to find the resulting type by applying type erasure.
          surfaceOf(alias.erasure)
        }
        val name     = symbol.asType.name.decodedName.toString
        val fullName = s"${prefix.typeSymbol.fullName}.${name}"
        q"wvlet.airframe.surface.Alias(${name}, ${fullName}, $inner)"
    }

    private val higherKindedTypeFactory: SurfaceFactory = {
      case t @ TypeRef(prefix, symbol, args) if t.typeArgs.isEmpty && t.takesTypeArgs =>
        // When higher-kinded types (e.g., Option[X], Future[X]) is passed as Option, Future without type arguments
        val name     = symbol.asType.name.decodedName.toString
        val fullName = s"${prefix.typeSymbol.fullName}.${name}"
        val inner    = surfaceOf(t.erasure)
        q"wvlet.airframe.surface.HigherKindedTypeSurface(${name}, ${fullName}, ${inner}, Seq.empty)"
      case t @ TypeRef(NoPrefix, tpe, List()) if tpe.name.decodedName.toString.contains("$") =>
        q"wvlet.airframe.surface.ExistentialType"
      case t @ TypeRef(NoPrefix, tpe, args) if !t.typeSymbol.isClass =>
        val fullName = tpe.fullName
        val name     = tpe.name.decodedName.toString
        val inner    = surfaceOf(t.erasure)
        val typeArgs = args.map(ta => surfaceOf(ta))
        q"wvlet.airframe.surface.HigherKindedTypeSurface(${name}, ${fullName}, ${inner}, Seq(..${typeArgs}))"
    }

    private val primitiveFactory: SurfaceFactory = {
      case t if t == typeOf[Short] => q"wvlet.airframe.surface.Primitive.Short"
      case t if t == typeOf[Boolean] =>
        q"wvlet.airframe.surface.Primitive.Boolean"
      case t if t == typeOf[Byte]  => q"wvlet.airframe.surface.Primitive.Byte"
      case t if t == typeOf[Char]  => q"wvlet.airframe.surface.Primitive.Char"
      case t if t == typeOf[Int]   => q"wvlet.airframe.surface.Primitive.Int"
      case t if t == typeOf[Float] => q"wvlet.airframe.surface.Primitive.Float"
      case t if t == typeOf[Long]  => q"wvlet.airframe.surface.Primitive.Long"
      case t if t == typeOf[Double] =>
        q"wvlet.airframe.surface.Primitive.Double"
      case t if t == typeOf[String] =>
        q"wvlet.airframe.surface.Primitive.String"
      case t if t == typeOf[java.lang.String] =>
        q"wvlet.airframe.surface.Primitive.String"
      case t if t == typeOf[Unit]                 => q"wvlet.airframe.surface.Primitive.Unit"
      case t if t == typeOf[BigInt]               => q"wvlet.airframe.surface.Primitive.BigInt"
      case t if t == typeOf[java.math.BigInteger] => q"wvlet.airframe.surface.Primitive.BigInteger"
    }

    private val arrayFactory: SurfaceFactory = {
      case t if typeNameOf(t) == "scala.Array" =>
        q"wvlet.airframe.surface.ArraySurface(classOf[$t], ${elementTypeOf(t)})"
    }

    private val optionFactory: SurfaceFactory = {
      case t if typeNameOf(t) == "scala.Option" =>
        q"wvlet.airframe.surface.OptionSurface(classOf[$t], ${elementTypeOf(t)})"
    }

    private val tupleFactory: SurfaceFactory = {
      case t if t <:< typeOf[Product] && t.typeSymbol.fullName.startsWith("scala.Tuple") =>
        val paramType = typeArgsOf(t).map(x => surfaceOf(x))
        q"new wvlet.airframe.surface.TupleSurface(classOf[$t], IndexedSeq(..$paramType))"
    }

    private val javaUtilFactory: SurfaceFactory = {
      case t
          if t =:= typeOf[java.io.File] ||
            t =:= typeOf[java.util.Date] ||
            t =:= typeOf[java.time.temporal.Temporal] =>
        q"new wvlet.airframe.surface.GenericSurface(classOf[$t])"
    }

    private val javaEnumFactory: SurfaceFactory = {
      case t if isEnum(t) =>
        q"wvlet.airframe.surface.JavaEnumSurface(classOf[$t])"
    }

    private def belongsToScalaDefault(t: c.Type) = {
      t match {
        case TypeRef(prefix, _, _) =>
          val scalaDefaultPackages =
            Seq("scala.", "scala.Predef.", "scala.util.")
          scalaDefaultPackages.exists(p => prefix.dealias.typeSymbol.fullName.startsWith(p))
        case _ => false
      }
    }

    private def isPhantomConstructor(constructor: Symbol): Boolean =
      constructor.asMethod.fullName.endsWith("$init$")

    def publicConstructorsOf(t: c.Type): Iterable[MethodSymbol] = {
      t.members
        .filter(m => m.isMethod && m.asMethod.isConstructor && m.isPublic)
        .filterNot(isPhantomConstructor)
        .map(
          _.asMethod
        )
    }

    def findPrimaryConstructorOf(t: c.Type): Option[MethodSymbol] = {
      publicConstructorsOf(t).find(x => x.isPrimaryConstructor)
    }

    def hasAbstractMethods(t: c.Type): Boolean =
      t.members.exists(x => x.isMethod && x.isAbstract && !x.isAbstractOverride)

    private def isAbstract(t: c.Type): Boolean = {
      t.typeSymbol.isAbstract && hasAbstractMethods(t)
    }

    private def isPathDependentType(t: c.Type): Boolean = {
      !t.typeSymbol.isStatic && t.toString.contains("#")
    }

    case class MethodArg(
        paramName: Symbol,
        tpe: c.Type,
        defaultValue: Option[c.Tree],
        isRequired: Boolean,
        isSecret: Boolean
    ) {
      def name: Literal = Literal(Constant(paramName.name.decodedName.toString))
      private def paramNameTerm = {
        TermName(paramName.name.encodedName.toString)
      }
      def typeSurface: c.Tree = surfaceOf(tpe)

      def isPrivateParam(t: c.Type): Boolean = {
        t.member(paramName.name) match {
          case NoSymbol => false
          case p        => p.isPrivate
        }
      }

      def accessor(t: c.Type): c.Tree = {
        try {
          if (
            paramName.isSynthetic || // x$1, etc.
            isPrivateParam(t) ||
            (t.typeSymbol.isAbstract && !(t <:< typeOf[AnyVal]))
          ) {
            q"None"
          } else {
            t.typeArgs.size match {
              // TODO We may need to expand Select(Ident(x.y.z....), TermName("a")) =>
              // Select(Select(Select(Ident(TermName("x")), TermName("y")), ....
              case 0 =>
                q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}].${paramNameTerm}})"
              case 1 =>
                q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_]].${paramNameTerm}})"
              case 2 =>
                q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_, _]].${paramNameTerm}})"
              case 3 =>
                q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_, _, _]].${paramNameTerm}})"
              case 4 =>
                q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_, _, _, _]].${paramNameTerm}})"
              case 5 =>
                q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_, _, _, _, _]].${paramNameTerm}})"
              case 6 =>
                q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_, _, _, _, _, _]].${paramNameTerm}})"
              case 7 =>
                q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_, _, _, _, _, _, _]].${paramNameTerm}})"
              case 8 =>
                q"Some({x:Any => x.asInstanceOf[${t.typeSymbol}[_, _, _, _, _, _, _, _]].${paramNameTerm}})"
              case other => q"None"
            }
          }
        } catch {
          case e: Throwable =>
            q"None"
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
      val classTypeParams = if (targetType.typeSymbol.isClass) {
        targetType.typeSymbol.asClass.typeParams
      } else {
        List.empty[Symbol]
      }

      val companion = targetType.companion match {
        case NoType => None
        case comp =>
          Some(comp)
      }

      // Exclude implicit ClassTag arguments
      val filteredParamLists = constructor.paramLists.map { params =>
        params.filter { x =>
          !x.typeSignature.toString.startsWith("scala.reflect.ClassTag[")
        }
      }

      val ret = for (params <- filteredParamLists) yield {
        val concreteArgTypes = params.map(_.typeSignature.substituteTypes(classTypeParams, targetType.typeArgs))
        var index            = 1
        for ((p, t) <- params.zip(concreteArgTypes)) yield {
          // Find the default argument of the method parameter
          val defaultValue =
            companion.flatMap { x =>
              // Find default value getter from the companion class
              val defaultValueGetter =
                findMethod(x, "apply$default$" + index)
                  .orElse(findMethod(x, "$lessinit$greater$default$" + index))
              // TODO: This doesn't work for local case class with default parameter values in Scala.js
              defaultValueGetter.map { method =>
                q"${method}"
              }
            }

          val isRequired = p.annotations.exists(_.tree.tpe == typeOf[required])
          val isSecret   = p.annotations.exists(_.tree.tpe == typeOf[secret])

          index += 1
          MethodArg(p, t, defaultValue, isRequired = isRequired, isSecret = isSecret)
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

    def methodParametersOf(targetType: c.Type, method: MethodSymbol): c.Tree = {
      val args = methodArgsOf(targetType, method).flatten
      methodParametersOf(targetType, method, args)
    }

    def methodParametersOf(targetType: c.Type, method: MethodSymbol, args: Seq[MethodArg]): c.Tree = {
      val argTypes = args.map { (x: MethodArg) =>
        // #913 dealias is necessary to resolve the actual type and type parameter
        val cls = toClassOf(x.tpe.dealias)
        cls
      }
      val ref =
        q"wvlet.airframe.surface.MethodRef(${toClassOf(targetType)}, ${method.name.decodedName.toString}, Seq(..$argTypes), ${method.isConstructor})"

      var index = 0
      val surfaceParams = args.map { arg =>
        val t = arg.name
        val defaultValue = arg.defaultValue match {
          case Some(x) => q"Some(${x})"
          case other   => q"None"
        }

        val accessor = if (method.isConstructor) {
          arg.accessor(targetType)
        } else {
          q"None"
        }

        val expr =
          q"""wvlet.airframe.surface.StaticMethodParameter(
            method = ${ref},
            index = ${index},
            name=${arg.name},
            isRequired = ${arg.isRequired},
            isSecret = ${arg.isSecret},
            surface = ${arg.typeSurface},
            defaultValue = ${defaultValue},
            accessor = ${accessor}
          )
          """
        index += 1
        expr
      }
      // Using IndexedSeq is necessary for Serialization
      q"IndexedSeq(..${surfaceParams})"
    }

    def createObjectFactoryOf(targetType: c.Type): Option[c.Tree] = {
      val ts = targetType.typeSymbol
      if (ts.isAbstract || ts.isModuleClass || isAbstract(targetType) || isPathDependentType(targetType)) {
        None
      } else {
        findPrimaryConstructorOf(targetType).map { primaryConstructor =>
          val argsList   = methodArgsOf(targetType, primaryConstructor)
          var index: Int = 0
          val argExtractor: List[List[c.Tree]] =
            for (arg <- argsList) yield {
              for (a <- arg) yield {
                val param =
                  Apply(Ident(TermName("args")), List(Literal(Constant(index))))
                index += 1
                // TODO natural type conversion (e.g., Int -> Long, etc.)
                q"${param}.asInstanceOf[${a.tpe}]"
              }
            }

          // Create a constructor call
          val id = Ident(targetType.typeSymbol)
          val constructor: c.Tree =
            argExtractor.foldLeft[c.Tree](Select(New(id), termNames.CONSTRUCTOR))((x, arg) => Apply(x, arg))

          // TODO: Support companion object call for instantiating the object
          val expr =
            q"""new wvlet.airframe.surface.ObjectFactory {
            def newInstance(args:Seq[Any]) : ${targetType} = { $constructor }
          }
          """
          expr
        }
      }
    }

    private val genericSurfaceWithConstructorFactory: SurfaceFactory =
      new SurfaceFactory {
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

          val expr = q"""
          new wvlet.airframe.surface.GenericSurface(
            classOf[$t],
            IndexedSeq(..$typeArgs),
            params = ${methodParametersOf(t, primaryConstructor)},
            objectFactory=${factory}
        )"""
          expr
        }
      }

    private val existentialTypeFactory: SurfaceFactory = { case t @ ExistentialType(quantified, underlying) =>
      surfaceOf(underlying)
    }

    private def finalTypeOf(t: c.Type): c.Type = {
      if (t.typeSymbol.asType.isAbstract && !(t =:= typeOf[AnyRef])) {
        // Use M[_] for type M
        t.erasure
      } else {
        t
      }
    }

    private def newGenericSurfaceOf(t: c.Type): c.Tree = {
      val expr =
        q"new wvlet.airframe.surface.GenericSurface(classOf[${finalTypeOf(t)}])"
      expr
    }

    private def hasStringUnapply(t: c.Type): Boolean = {
      t.companion match {
        case companion: Type =>
          // Find unapply(String): Option[X]
          companion.member(TermName("unapply")) match {
            case s: Symbol if s.isMethod && s.asMethod.paramLists.size == 1 =>
              val m    = s.asMethod
              val args = m.paramLists.head
              args.size == 1 &&
              args.head.typeSignature =:= typeOf[String] &&
              m.returnType <:< weakTypeOf[Option[_]] &&
              m.returnType.typeArgs.size == 1 &&
              m.returnType.typeArgs.head =:= t
            case _ => false
          }
        case _ => false
      }
    }

    private val genericSurfaceFactory: SurfaceFactory = {
      case t @ TypeRef(prefix, symbol, args) if !args.isEmpty =>
        val typeArgs = typeArgsOf(t).map(surfaceOf(_))
        q"new wvlet.airframe.surface.GenericSurface(classOf[$t], typeArgs = IndexedSeq(..$typeArgs))"
      case t @ TypeRef(NoPrefix, symbol, args) if !t.typeSymbol.isClass =>
        q"wvlet.airframe.surface.ExistentialType"
      case t @ RefinedType(List(_, baseType), decl) =>
        newGenericSurfaceOf(baseType)
      case t if hasStringUnapply(t) =>
        t.typeSymbol.fullName.split("\\.").toList match {
          case p1 :: tl =>
            // companion object package -> Select(Select( .... ))) tree
            val ex = tl.foldLeft[Tree](Ident(TermName(p1))) { (z, n) =>
              Select(z, TermName(n))
            }
            val expr =
              q"""wvlet.airframe.surface.EnumSurface(classOf[${finalTypeOf(t)}], { (cl: Class[_], s: String) =>
                ${ex}.unapply(s).asInstanceOf[Option[Any]]
              }
            )"""
            expr
          case _ =>
            newGenericSurfaceOf(t)
        }
      case t =>
        newGenericSurfaceOf(t)
    }

    private val surfaceFactories: SurfaceFactory =
      primitiveFactory orElse
        taggedTypeFactory orElse
        aliasFactory orElse
        higherKindedTypeFactory orElse
        arrayFactory orElse
        optionFactory orElse
        tupleFactory orElse
        javaUtilFactory orElse
        javaEnumFactory orElse
        genericSurfaceWithConstructorFactory orElse
        existentialTypeFactory orElse
        genericSurfaceFactory

    def surfaceOf(t: c.Type): c.Tree = {
      if (seen.contains(t)) {
        if (memo.contains(t)) {
          memo(t)
        } else {
          q"wvlet.airframe.surface.LazySurface(classOf[${t}], ${fullTypeNameOf(t)})"
        }
      } else {
        seen += t
        // We don't need to cache primitive types
        val surfaceGenerator =
          surfaceFactories andThen { tree =>
            // cache the generated Surface instance
            q"wvlet.airframe.surface.surfaceCache.getOrElseUpdate(${fullTypeNameOf(t)}, ${tree})"
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
      val name = typeEv match {
        case TypeRef(prefix, typeSymbol, args) if args.isEmpty =>
          typeSymbol.fullName
        case TypeRef(prefix, typeSymbol, args) if !args.isEmpty =>
          val typeArgs = args.map(fullTypeNameOf(_)).mkString(",")
          s"${typeSymbol.fullName}[${typeArgs}]"
        case _ => typeEv.typeSymbol.fullName
      }
      sanitizeTypeName(name)
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
