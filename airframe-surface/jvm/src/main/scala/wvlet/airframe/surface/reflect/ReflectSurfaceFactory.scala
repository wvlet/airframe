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
package wvlet.airframe.surface.reflect

import java.lang.reflect.{Constructor, InvocationTargetException}
import java.util.concurrent.ConcurrentHashMap

import wvlet.log.LogSupport
import wvlet.airframe.surface._

import scala.jdk.CollectionConverters._
import scala.reflect.runtime.{universe => ru}

/**
  *
  */
object ReflectSurfaceFactory extends LogSupport {
  import ru._

  private type TypeName = String

  private[surface] val surfaceCache       = new ConcurrentHashMap[TypeName, Surface].asScala
  private[surface] val methodSurfaceCache = new ConcurrentHashMap[TypeName, Seq[MethodSurface]].asScala
  private[surface] val typeMap            = new ConcurrentHashMap[Surface, ru.Type].asScala

  private def belongsToScalaDefault(t: ru.Type) = {
    t match {
      case ru.TypeRef(prefix, _, _) =>
        val scalaDefaultPackages = Seq("scala.", "scala.Predef.", "scala.util.")
        scalaDefaultPackages.exists(p => prefix.dealias.typeSymbol.fullName.startsWith(p))
      case _ => false
    }
  }

  def of[A: ru.WeakTypeTag]: Surface = ofType(implicitly[ru.WeakTypeTag[A]].tpe)

  def ofType(tpe: ru.Type): Surface = {
    apply(tpe)
  }
  def ofClass(cls: Class[_]): Surface = {
    val tpe = scala.reflect.runtime.currentMirror.classSymbol(cls).toType
    ofType(tpe)
  }

  private def getPrimaryConstructorOf(cls: Class[_]): Option[Constructor[_]] = {
    val constructors = cls.getConstructors
    if (constructors.size == 0) {
      None
    } else {
      Some(constructors(0))
    }
  }

  private def getFirstParamTypeOfPrimaryConstructor(cls: Class[_]): Option[Class[_]] = {
    getPrimaryConstructorOf(cls).flatMap { constructor =>
      val constructorParamTypes = constructor.getParameterTypes
      if (constructorParamTypes.size == 0) {
        None
      } else {
        Some(constructorParamTypes(0))
      }
    }
  }

  def localSurfaceOf[A: ru.WeakTypeTag](context: Any): Surface = {
    val tpe = implicitly[ru.WeakTypeTag[A]].tpe
    ofType(tpe) match {
      case r: RuntimeGenericSurface =>
        getFirstParamTypeOfPrimaryConstructor(r.rawType) match {
          case Some(outerClass) if outerClass == context.getClass =>
            // Add outer context class to the Surface to support Surface.objectFactory -> newInstance(outer, p1, p2, ...)
            r.withOuter(context.asInstanceOf[AnyRef])
          case _ =>
            // In this surface, we cannot support objectFactory, but param etc. will work
            r
        }
      case other => other
    }
  }

  def findTypeOf(s: Surface): Option[ru.Type] = typeMap.get(s)

  def get(name: String): Surface = {
    surfaceCache.getOrElse(name, throw new IllegalArgumentException(s"Surface ${name} is not found in cache"))
  }

  private def typeNameOf(t: ru.Type): String = {
    t.dealias.typeSymbol.fullName
  }

  private def isTaggedType(t: ru.Type): Boolean = {
    typeNameOf(t).startsWith("wvlet.airframe.surface.tag.")
  }

  private def fullTypeNameOf(tpe: ru.Type): TypeName = {
    tpe match {
      case t if t.typeArgs.length == 2 && isTaggedType(t) =>
        s"${fullTypeNameOf(t.typeArgs(0))}@@${fullTypeNameOf(t.typeArgs(1))}"
      case alias @ TypeRef(prefix, symbol, args)
          if symbol.isType &&
            symbol.asType.isAliasType &&
            !belongsToScalaDefault(alias) =>
        val name     = symbol.asType.name.decodedName.toString
        val fullName = s"${prefix.typeSymbol.fullName}.${name}"
        fullName
      case TypeRef(prefix, typeSymbol, args) if args.isEmpty => typeSymbol.fullName
      case TypeRef(prefix, typeSymbol, args) if !args.isEmpty =>
        val typeArgs = args.map(fullTypeNameOf(_)).mkString(",")
        s"${typeSymbol.fullName}[${typeArgs}]"
      case _ => tpe.typeSymbol.fullName
    }
  }

  def apply(tpe: ru.Type): Surface = {
    surfaceCache.getOrElseUpdate(fullTypeNameOf(tpe), new SurfaceFinder().surfaceOf(tpe))
  }

  def methodsOf(s: Surface): Seq[MethodSurface] = {
    findTypeOf(s)
      .map { tpe =>
        methodsOfType(tpe)
      }
      .getOrElse(Seq.empty)
  }

  def methodsOf[A: ru.WeakTypeTag]: Seq[MethodSurface] = methodsOfType(implicitly[ru.WeakTypeTag[A]].tpe)

  def methodsOfType(tpe: ru.Type): Seq[MethodSurface] = {
    methodSurfaceCache.getOrElseUpdate(fullTypeNameOf(tpe), {
      new SurfaceFinder().createMethodSurfaceOf(tpe)
    })
  }

  def methodsOfClass(cls: Class[_]): Seq[MethodSurface] = {
    val tpe = scala.reflect.runtime.currentMirror.classSymbol(cls).toType
    methodsOfType(tpe)
  }

  private[surface] def mirror = ru.runtimeMirror(Thread.currentThread.getContextClassLoader)
  private def resolveClass(tpe: ru.Type): Class[_] = {
    try {
      mirror.runtimeClass(tpe)
    } catch {
      case e: Throwable => classOf[Any]
    }
  }

  def hasAbstractMethods(t: ru.Type): Boolean =
    t.members.exists(x => x.isMethod && x.isAbstract && !x.isAbstractOverride)

  private def isAbstract(t: ru.Type): Boolean = {
    t.typeSymbol.isAbstract && hasAbstractMethods(t)
  }

  private type SurfaceMatcher = PartialFunction[ru.Type, Surface]

  private class SurfaceFinder extends LogSupport {
    private val seen       = scala.collection.mutable.Set[ru.Type]()
    private val methodSeen = scala.collection.mutable.Set[ru.Type]()

    private def allMethodsOf(t: ru.Type): Iterable[MethodSymbol] = {
      t.members.sorted // Sort the members in the source code order
        .filter(x =>
          x.isMethod &&
            !x.isConstructor &&
            !x.isImplementationArtifact
            && !x.isMacro
            && !x.isImplicit
          // synthetic is used for functions returning default values of method arguments (e.g., ping$default$1)
            && !x.isSynthetic
        )
        .map(_.asMethod)
        .filter { x =>
          val name = x.name.decodedName.toString
          !x.isAccessor && !name.startsWith("$") && name != "<init>"
        }
    }

    def localMethodsOf(t: ru.Type): Iterable[MethodSymbol] = {
      allMethodsOf(t)
        .filter(m => isOwnedByTargetClass(m, t))
    }

    private def nonObject(x: ru.Symbol): Boolean = {
      !x.isImplementationArtifact &&
      !x.isSynthetic &&
      //!x.isAbstract &&
      x.fullName != "scala.Any" &&
      x.fullName != "java.lang.Object"
    }

    private def isOwnedByTargetClass(m: MethodSymbol, t: ru.Type): Boolean = {
      m.owner == t.typeSymbol || t.baseClasses.filter(nonObject).exists(_ == m.owner)
    }

    def createMethodSurfaceOf(targetType: ru.Type): Seq[MethodSurface] = {
      val name = fullTypeNameOf(targetType)
      if (methodSurfaceCache.contains(name)) {
        methodSurfaceCache(name)
      } else if (methodSeen.contains(targetType)) {
        throw new IllegalArgumentException(s"recursive type in method: ${targetType.typeSymbol.fullName}")
      } else {
        methodSeen += targetType
        val methodSurfaces = {
          val localMethods = targetType match {
            case t @ TypeRef(prefix, typeSymbol, typeArgs) =>
              localMethodsOf(t.dealias)
            case t @ RefinedType(List(_, baseType), decls: MemberScope) =>
              localMethodsOf(baseType) ++ localMethodsOf(t)
            case _ => Seq.empty
          }
          val list = for (m <- localMethods) yield {
            val mod   = modifierBitMaskOf(m)
            val owner = surfaceOf(targetType)
            val name  = m.name.decodedName.toString
            val ret   = surfaceOf(m.returnType)
            val args  = methodParametersOf(targetType, m)
            ReflectMethodSurface(mod, owner, name, ret, args.toIndexedSeq)
          }
          list.toIndexedSeq
        }
        methodSurfaceCache += name -> methodSurfaces
        methodSurfaces
      }
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

    def surfaceOf(tpe: ru.Type): Surface = {
      try {
        val fullName = fullTypeNameOf(tpe)
        if (surfaceCache.contains(fullName)) {
          surfaceCache(fullName)
        } else if (seen.contains(tpe)) {
          // Recursive type
          LazySurface(resolveClass(tpe), fullName, typeArgsOf(tpe).map(x => surfaceOf(x)))
        } else {
          seen += tpe
          val m = surfaceFactories.orElse[ru.Type, Surface] {
            case _ =>
              trace(f"Resolving the unknown type $tpe into AnyRef")
              new GenericSurface(resolveClass(tpe))
          }
          val surface = m(tpe)
          // Cache if not yet cached
          surfaceCache.getOrElseUpdate(fullName, surface)
          typeMap.getOrElseUpdate(surface, tpe)
          surface
        }
      } catch {
        case e: Throwable =>
          error(s"Failed to build Surface.of[${tpe}]", e)
          throw e
      }
    }

    private val surfaceFactories: SurfaceMatcher =
      taggedTypeFactory orElse
        aliasFactory orElse
        higherKindedTypeFactory orElse
        primitiveTypeFactory orElse
        arrayFactory orElse
        optionFactory orElse
        tupleFactory orElse
        javaUtilFactory orElse
        enumFactory orElse
        genericSurfaceWithConstructorFactory orElse
        existentialTypeFactory orElse
        genericSurfaceFactory

    private def primitiveTypeFactory: SurfaceMatcher = {
      case t if t =:= typeOf[String]  => Primitive.String
      case t if t =:= typeOf[Boolean] => Primitive.Boolean
      case t if t =:= typeOf[Int]     => Primitive.Int
      case t if t =:= typeOf[Long]    => Primitive.Long
      case t if t =:= typeOf[Float]   => Primitive.Float
      case t if t =:= typeOf[Double]  => Primitive.Double
      case t if t =:= typeOf[Short]   => Primitive.Short
      case t if t =:= typeOf[Byte]    => Primitive.Byte
      case t if t =:= typeOf[Char]    => Primitive.Char
      case t if t =:= typeOf[Unit]    => Primitive.Unit
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

    private def higherKindedTypeFactory: SurfaceMatcher = {
      case t @ TypeRef(prefix, symbol, args) if t.typeArgs.isEmpty && t.takesTypeArgs =>
        // When higher-kinded types (e.g., Option[X], Future[X]) is passed as Option, Future without type arguments
        val inner    = surfaceOf(t.erasure)
        val name     = symbol.asType.name.decodedName.toString
        val fullName = s"${prefix.typeSymbol.fullName}.${name}"
        HigherKindedTypeSurface(name, fullName, inner)
    }

    private def taggedTypeFactory: SurfaceMatcher = {
      case t if t.typeArgs.length == 2 && typeNameOf(t).startsWith("wvlet.airframe.surface.tag.") =>
        TaggedSurface(surfaceOf(t.typeArgs(0)), surfaceOf(t.typeArgs(1)))
    }

    private def aliasFactory: SurfaceMatcher = {
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
        val a        = Alias(name, fullName, inner)
        a
    }

    private def arrayFactory: SurfaceMatcher = {
      case t if typeNameOf(t) == "scala.Array" =>
        ArraySurface(resolveClass(t), elementTypeOf(t))
    }

    private def optionFactory: SurfaceMatcher = {
      case t if typeNameOf(t) == "scala.Option" =>
        OptionSurface(resolveClass(t), elementTypeOf(t))
    }

    private def tupleFactory: SurfaceMatcher = {
      case t if t <:< typeOf[Product] && t.typeSymbol.fullName.startsWith("scala.Tuple") =>
        val paramType = typeArgsOf(t).map(x => surfaceOf(x))
        TupleSurface(resolveClass(t), paramType.toIndexedSeq)
    }

    private def javaUtilFactory: SurfaceMatcher = {
      case t
          if t =:= typeOf[java.io.File] ||
            t =:= typeOf[java.util.Date] ||
            t =:= typeOf[java.time.temporal.Temporal] ||
            t =:= typeOf[Throwable] ||
            t =:= typeOf[Exception] ||
            t =:= typeOf[Error] =>
        new GenericSurface(resolveClass(t))
    }

    private def isEnum(t: ru.Type): Boolean = {
      t.baseClasses.exists { x =>
        if (x.isJava && x.isType) {
          x.asType.fullName.toString.startsWith("java.lang.Enum")
        } else {
          false
        }
      }
    }

    private def enumFactory: SurfaceMatcher = {
      case t if isEnum(t) =>
        EnumSurface(resolveClass(t))
    }

    def hasAbstractMethods(t: ru.Type): Boolean =
      t.members.exists(x => x.isMethod && x.isAbstract && !x.isAbstractOverride)

    private def isAbstract(t: ru.Type): Boolean = {
      t.typeSymbol.isAbstract && hasAbstractMethods(t)
    }

    private def isPhantomConstructor(constructor: Symbol): Boolean =
      constructor.asMethod.fullName.endsWith("$init$")

    def publicConstructorsOf(t: ru.Type): Iterable[MethodSymbol] = {
      t.members
        .filter(m => m.isMethod && m.asMethod.isConstructor && m.isPublic).filterNot(isPhantomConstructor).map(
          _.asMethod
        )
    }

    def findPrimaryConstructorOf(t: ru.Type): Option[MethodSymbol] = {
      publicConstructorsOf(t).find(x => x.isPrimaryConstructor)
    }

    case class MethodArg(paramName: Symbol, tpe: ru.Type) {
      def name: String         = paramName.name.decodedName.toString
      def typeSurface: Surface = surfaceOf(tpe)
    }

    private def findMethod(m: ru.Type, name: String): Option[MethodSymbol] = {
      m.member(ru.TermName(name)) match {
        case ru.NoSymbol => None
        case other       => Some(other.asMethod)
      }
    }

    private def methodArgsOf(targetType: ru.Type, constructor: MethodSymbol): List[List[MethodArg]] = {
      val classTypeParams = if (targetType.typeSymbol.isClass) {
        targetType.typeSymbol.asClass.typeParams
      } else {
        List.empty[Symbol]
      }

      val companion = targetType.companion match {
        case NoType => None
        case comp   => Some(comp)
      }

      for (params <- constructor.paramLists) yield {
        val concreteArgTypes = params.map { p =>
          try {
            p.typeSignature.substituteTypes(classTypeParams, targetType.typeArgs)
          } catch {
            case e: Throwable =>
              p.typeSignature
          }
        }
        var index = 1
        for ((p, t) <- params.zip(concreteArgTypes)) yield {
          index += 1
          MethodArg(p, t)
        }
      }
    }

    def methodParametersOf(targetType: ru.Type, method: MethodSymbol): Seq[RuntimeMethodParameter] = {
      val args = methodArgsOf(targetType, method).flatten
      val argTypes = args.map { x: MethodArg =>
        resolveClass(x.tpe)
      }.toSeq
      val ref = MethodRef(resolveClass(targetType), method.name.decodedName.toString, argTypes, method.isConstructor)

      var index = 0
      val surfaceParams = args.map { arg =>
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

    private def genericSurfaceWithConstructorFactory: SurfaceMatcher = new SurfaceMatcher with LogSupport {
      override def isDefinedAt(t: ru.Type): Boolean = {
        !isAbstract(t) && findPrimaryConstructorOf(t).exists(!_.paramLists.isEmpty)
      }
      override def apply(t: ru.Type): Surface = {
        val primaryConstructor = findPrimaryConstructorOf(t).get
        val typeArgs           = typeArgsOf(t).map(surfaceOf(_)).toIndexedSeq
        val methodParams       = methodParametersOf(t, primaryConstructor)

        val s = new RuntimeGenericSurface(
          resolveClass(t),
          typeArgs,
          params = methodParams
        )
        s
      }
    }

    private def existentialTypeFactory: SurfaceMatcher = {
      case t @ ru.ExistentialType(quantified, underlying) =>
        surfaceOf(underlying)
    }

    private def genericSurfaceFactory: SurfaceMatcher = {
      case t @ TypeRef(prefix, symbol, args) if !args.isEmpty =>
        val typeArgs = typeArgsOf(t).map(surfaceOf(_)).toIndexedSeq
        new GenericSurface(resolveClass(t), typeArgs = typeArgs)
      case t @ TypeRef(NoPrefix, symbol, args) if !t.typeSymbol.isClass =>
        wvlet.airframe.surface.ExistentialType
      case t @ TypeRef(prefix, symbol, args) if resolveClass(t) == classOf[AnyRef] && !(t =:= typeOf[AnyRef]) =>
        // For example, trait MyTag, which has no implementation will be just an java.lang.Object
        val name     = t.typeSymbol.name.decodedName.toString
        val fullName = s"${prefix.typeSymbol.fullName}.${name}"
        Alias(name, fullName, AnyRefSurface)
      case t @ RefinedType(List(_, baseType), decl) =>
        // For traits with extended methods
        new GenericSurface(resolveClass(baseType))
      case t =>
        new GenericSurface(resolveClass(t))
    }
  }

  /**
    * Used when we can use reflection to instantiate objects of this surface
    *
    * @param rawType
    * @param typeArgs
    * @param params
    */
  class RuntimeGenericSurface(
      override val rawType: Class[_],
      override val typeArgs: Seq[Surface] = Seq.empty,
      override val params: Seq[Parameter] = Seq.empty,
      outer: Option[AnyRef] = None
  ) extends GenericSurface(rawType, typeArgs, params, None)
      with LogSupport {
    self =>

    def withOuter(outer: AnyRef): Surface = {
      new RuntimeGenericSurface(rawType, typeArgs, params, Some(outer))
    }

    private class ReflectObjectFactory extends ObjectFactory {
      private lazy val isStatic = mirror.classSymbol(rawType).isStatic
      private def outerInstance: Option[AnyRef] = {
        if (isStatic) {
          None
        } else {
          // Inner class
          outer.orElse {
            val contextClass = getFirstParamTypeOfPrimaryConstructor(rawType)
            val msg = contextClass
              .map(x =>
                s" Call Surface.of[${rawType.getSimpleName}] or bind[${rawType.getSimpleName}].toXXX where `this` points to an instance of ${x}"
              ).getOrElse(
                ""
              )
            throw new IllegalStateException(
              s"Cannot build a non-static class ${rawType.getName}.${msg}"
            )
          }
        }
      }

      // Create instance with Reflection
      override def newInstance(args: Seq[Any]): Any = {
        try {
          // We should not store the primary constructor reference here to avoid including java.lang.reflect.Constructor,
          // which is non-serializable, within this RuntimeGenericSurface class
          getPrimaryConstructorOf(rawType)
            .map { primaryConstructor =>
              val argList = Seq.newBuilder[AnyRef]
              if (!isStatic) {
                // Add a reference to the context instance if this surface represents an inner class
                outerInstance.foreach { x =>
                  argList += x
                }
              }
              argList ++= args.map(_.asInstanceOf[AnyRef])
              val a = argList.result()
              if (a.isEmpty) {
                logger.trace(s"build ${rawType.getName} using the default constructor")
                primaryConstructor.newInstance()
              } else {
                logger.trace(s"build ${rawType.getName} with args: ${a.mkString(", ")}")
                primaryConstructor.newInstance(a: _*)
              }
            }
            .getOrElse {
              throw new IllegalStateException(s"No primary constructor is found for ${rawType}")
            }
        } catch {
          case e: InvocationTargetException =>
            logger.warn(
              s"Failed to instantiate ${self}: [${e.getTargetException.getClass.getName}] ${e.getTargetException.getMessage}\nargs:[${args
                .mkString(", ")}]"
            )
            throw e.getTargetException
          case e: Throwable =>
            logger.warn(
              s"Failed to instantiate ${self}: [${e.getClass.getName}] ${e.getMessage}\nargs:[${args.mkString(", ")}]"
            )
            throw e
        }
      }
    }

    override val objectFactory: Option[ObjectFactory] = {
      if (rawType.getConstructors.isEmpty) {
        None
      } else {
        Some(new ReflectObjectFactory())
      }
    }
  }
}
