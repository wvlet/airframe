package wvlet.airframe.surface
import scala.quoted._
import dotty.tools.dotc.core.{Types as DottyTypes}

private[surface] object CompileTimeSurfaceFactory {

  type SurfaceMatcher = PartialFunction[Type[_], Expr[Surface]]

  def surfaceOf[A](using tpe: Type[A], quotes: Quotes): Expr[Surface] = {
    import quotes._
    import quotes.reflect._

    val f           = new CompileTimeSurfaceFactory(using quotes)
    val surfaceExpr = f.surfaceOf(tpe)
    val t           = TypeRepr.of[A]
    val flags       = t.typeSymbol.flags
    if (!flags.is(Flags.Static) && flags.is(Flags.NoInits)) {
      t.typeSymbol.maybeOwner match {
        case s: Symbol
            if !s.isNoSymbol &&
              s.isClassDef &&
              !s.isPackageDef &&
              !s.flags.is(Flags.Module) &&
              !s.flags.is(Flags.Trait) =>
          // println(s"${t}\n${flags.show}\nowner:${s}\n${s.flags.show}")
          '{ ${ surfaceExpr }.withOuter(${ This(s).asExpr }.asInstanceOf[AnyRef]) }
        case _ =>
          surfaceExpr
      }
    } else {
      surfaceExpr
    }
  }

  def methodsOf[A](using tpe: Type[A], quotes: Quotes): Expr[Seq[MethodSurface]] = {
    val f = new CompileTimeSurfaceFactory(using quotes)
    f.methodsOf(tpe)
  }
}

private[surface] class CompileTimeSurfaceFactory[Q <: Quotes](using quotes: Q) {
  import quotes._
  import quotes.reflect._

  private def fullTypeNameOf(t: Type[_]): String = {
    fullTypeNameOf(TypeRepr.of(using t))
  }

  private def fullTypeNameOf(t: TypeRepr): String = {
    def sanitize(symbol: Symbol): String = {
      val nameParts: List[String] = symbol.fullName.split("\\.").toList match {
        case "scala" :: "Predef$" :: tail =>
          tail
        case "scala" :: "collection" :: "immutable" :: tail =>
          tail
        case "scala" :: nme :: Nil =>
          List(nme)
        case other =>
          other
      }
      nameParts
        .mkString(".").stripSuffix("$").replaceAll("\\.package\\$", ".").replaceAll("\\$+", ".")
        .replaceAll("\\.\\.", ".")
    }
    t match {
      case a: AppliedType if a.args.nonEmpty =>
        s"${sanitize(a.typeSymbol)}[${a.args.map(pt => fullTypeNameOf(pt.asType)).mkString(",")}]"
      case other =>
        sanitize(other.typeSymbol)
    }
  }

  private type Factory = PartialFunction[TypeRepr, Expr[Surface]]

  def surfaceOf(tpe: Type[_]): Expr[Surface] = {
    surfaceOf(TypeRepr.of(using tpe))
  }

  private val seen = scala.collection.mutable.Set[TypeRepr]()
  private val memo = scala.collection.mutable.Map[TypeRepr, Expr[Surface]]()

  private def surfaceOf(t: TypeRepr): Expr[Surface] = {
    // println(s"surfaceOf ${fullTypeNameOf(t)}")
    if (seen.contains(t)) {
      if (memo.contains(t)) {
        memo(t)
      } else {
        '{ LazySurface(${ clsOf(t) }, ${ Expr(fullTypeNameOf(t)) }) }
      }
    } else {
      seen += t
      // For debugging
      // println(s"[${typeNameOf(t)}]\n  ${t}")
      val generator = factory.andThen { expr =>
        val cacheKey =
          if (typeNameOf(t) == "scala.Any" && classOf[DottyTypes.TypeParamRef].isAssignableFrom(t.getClass)) {
            // This ensure different cache key for each Type Parameter (such as T and U).
            // This is required because fullTypeNameOf of every Type Parameters is `scala.Any`.
            s"${fullTypeNameOf(t)} for ${t}"
          } else {
            fullTypeNameOf(t)
          }
        '{ wvlet.airframe.surface.surfaceCache.getOrElseUpdate(${ Expr(cacheKey) }, ${ expr }) }
      }
      val surface = generator(t)
      // println(s"--- ${surface.show}")
      memo += (t -> surface)
      surface
    }
  }

  private def factory: Factory = {
    taggedTypeFactory orElse
      aliasFactory orElse
      higherKindedTypeFactory orElse
      primitiveTypeFactory orElse
      arrayFactory orElse
      optionFactory orElse
      tupleFactory orElse
      javaUtilFactory orElse
      javaEnumFactory orElse
      exisitentialTypeFactory orElse
      genericTypeWithConstructorFactory orElse
      typeParameterFactory orElse
      genericTypeFactory
  }

  private def primitiveTypeFactory: Factory = {
    case t if t =:= TypeRepr.of[String]               => '{ Primitive.String }
    case t if t =:= TypeRepr.of[Boolean]              => '{ Primitive.Boolean }
    case t if t =:= TypeRepr.of[Int]                  => '{ Primitive.Int }
    case t if t =:= TypeRepr.of[Long]                 => '{ Primitive.Long }
    case t if t =:= TypeRepr.of[Float]                => '{ Primitive.Float }
    case t if t =:= TypeRepr.of[Double]               => '{ Primitive.Double }
    case t if t =:= TypeRepr.of[Short]                => '{ Primitive.Short }
    case t if t =:= TypeRepr.of[Byte]                 => '{ Primitive.Byte }
    case t if t =:= TypeRepr.of[Char]                 => '{ Primitive.Char }
    case t if t =:= TypeRepr.of[Unit]                 => '{ Primitive.Unit }
    case t if t =:= TypeRepr.of[BigInt]               => '{ Primitive.BigInt }
    case t if t =:= TypeRepr.of[java.math.BigInteger] => '{ Primitive.BigInteger }
  }

  private def typeNameOf(t: TypeRepr): String = {
    t.typeSymbol.fullName.stripSuffix("$").replaceAll("\\.package\\$", ".").replaceAll("\\$+", ".")
  }

  private def isTaggedType(t: TypeRepr): Boolean = {
    typeNameOf(t).startsWith("wvlet.airframe.surface.tag.")
  }

  private def taggedTypeFactory: Factory = {
    case a: AppliedType if a.args.length == 2 && isTaggedType(a) =>
      '{ TaggedSurface(${ surfaceOf(a.args(0)) }, ${ surfaceOf(a.args(1)) }) }
  }

  private def belongsToScalaDefault(t: TypeRepr): Boolean = {
    val scalaDefaultPackages = Seq("scala.", "scala.Predef$.", "scala.util.")
    val nme                  = t.typeSymbol.fullName
    scalaDefaultPackages.exists(p => nme.startsWith(p))
  }

  private def aliasFactory: Factory = {
    case t if t.typeSymbol.isType && t.typeSymbol.isAliasType && !belongsToScalaDefault(t) =>
      val dealiased = t.dealias
      val inner = if (t != dealiased) {
        surfaceOf(dealiased)
      } else {
        surfaceOf(t.simplified)
      }
      val s        = t.typeSymbol
      val name     = Expr(s.name)
      val fullName = Expr(fullTypeNameOf(t.asType))
      '{ Alias(${ name }, ${ fullName }, ${ inner }) }
  }

  private def higherKindedTypeFactory: Factory = {
    case h: TypeLambda =>
      val name     = h.typeSymbol.name
      val fullName = fullTypeNameOf(h)
      val inner    = surfaceOf(h.resType)

      val len    = h.paramNames.size
      val params = (0 until len).map { i => h.param(i) }
      val args   = params.map(surfaceOf(_))
      '{ HigherKindedTypeSurface(${ Expr(name) }, ${ Expr(fullName) }, ${ inner }, ${ Expr.ofSeq(args) }) }
    case a @ AppliedType if a.typeSymbol.name.contains("$") =>
      '{ wvlet.airframe.surface.ExistentialType }
    case a: AppliedType if !a.typeSymbol.isClassDef =>
      val name     = a.typeSymbol.name
      val fullName = fullTypeNameOf(a)
      val args     = a.args.map(surfaceOf(_))
      // TODO support type erasure instead of using AnyRefSurface
      '{ HigherKindedTypeSurface(${ Expr(name) }, ${ Expr(fullName) }, AnyRefSurface, ${ Expr.ofSeq(args) }) }
  }

  private def typeArgsOf(t: TypeRepr): List[TypeRepr] = {
    t match {
      case a: AppliedType =>
        a.args
      case other =>
        List.empty
    }
  }

  private def elementTypeSurfaceOf(t: TypeRepr): Expr[Surface] = {
    typeArgsOf(t).map(surfaceOf(_)).head
  }

  private def arrayFactory: Factory = {
    case t if typeNameOf(t) == "scala.Array" =>
      '{ ArraySurface(${ clsOf(t) }, ${ elementTypeSurfaceOf(t) }) }
  }

  private def optionFactory: Factory = {
    case t if typeNameOf(t) == "scala.Option" =>
      '{ OptionSurface(${ clsOf(t) }, ${ elementTypeSurfaceOf(t) }) }
  }

  private def tupleFactory: Factory = {
    case t if t <:< TypeRepr.of[Product] && typeNameOf(t).startsWith("scala.Tuple") =>
      val paramTypes = typeArgsOf(t).map(surfaceOf(_))
      '{ new TupleSurface(${ clsOf(t) }, ${ Expr.ofSeq(paramTypes) }.toIndexedSeq) }
  }

  private def javaUtilFactory: Factory = {
    // For common Java classes, stop with this rule so as not to extract internal parameters
    case t
        if t =:= TypeRepr.of[java.io.File] ||
          t =:= TypeRepr.of[java.util.Date] ||
          t =:= TypeRepr.of[java.time.temporal.Temporal] =>
      newGenericSurfaceOf(t)
  }

  private def isEnum(t: TypeRepr): Boolean = {
    t.baseClasses.exists(x => x.fullName.startsWith("java.lang.Enum"))
  }

  private def javaEnumFactory: Factory = {
    case t if isEnum(t) =>
      '{ JavaEnumSurface(${ clsOf(t) }) }
  }

  private def exisitentialTypeFactory: Factory = {
    case t: TypeBounds if t.hi =:= TypeRepr.of[Any] =>
      // TODO Represent low/hi bounds
      '{ ExistentialType }
  }

  private def clsOf(t: TypeRepr): Expr[Class[_]] = {
    Literal(ClassOfConstant(t)).asExpr.asInstanceOf[Expr[Class[_]]]
  }

  private def newGenericSurfaceOf(t: TypeRepr): Expr[Surface] = {
    '{ new GenericSurface(${ clsOf(t) }) }
  }

  private def genericTypeWithConstructorFactory: Factory = {
    case t
        if !t.typeSymbol.flags.is(Flags.Abstract) && !t.typeSymbol.flags.is(Flags.Trait)
          && Option(t.typeSymbol.primaryConstructor).exists(p => p.exists && p.paramSymss.nonEmpty) =>
      val typeArgs     = typeArgsOf(t.simplified).map(surfaceOf(_))
      val methodParams = constructorParametersOf(t)
      val isStatic     = !t.typeSymbol.flags.is(Flags.Local)
      // TODO: This code doesn't work for Scala.js + Scala 3.0.0
      '{
        new wvlet.airframe.surface.reflect.RuntimeGenericSurface(
          ${ clsOf(t) },
          ${ Expr.ofSeq(typeArgs) }.toIndexedSeq,
          params = ${ methodParams },
          isStatic = ${ Expr(isStatic) }
        )
      }
  }

  private def typeParameterFactory: Factory = {
    case p: DottyTypes.ParamRef if (fullTypeNameOf(p) == "Any") =>
      val paramName = Expr(p.paramName.toString)
      '{ HigherKindedTypeSurface(${ paramName }, ${ paramName }, AnyRefSurface, Nil) }
  }

  private def genericTypeFactory: Factory = {
    case a: AppliedType =>
      val typeArgs = a.args.map(surfaceOf(_))
      '{ new GenericSurface(${ clsOf(a) }, typeArgs = ${ Expr.ofSeq(typeArgs) }.toIndexedSeq) }
    case r: Refinement =>
      newGenericSurfaceOf(r.info)
    case t if hasStringUnapply(t) =>
      '{
        EnumSurface(
          ${ clsOf(t) },
          { (cl: Class[_], s: String) => wvlet.airframe.surface.reflect.TypeConverter.convertToCls(s, cl) }
        )
      }
    case t =>
      newGenericSurfaceOf(t)
  }

  private def hasOptionReturnType(d: DefDef, retElementType: TypeRepr): Boolean = {
    if (d.returnTpt.tpe <:< TypeRepr.of[Option[_]]) {
      val typeArgs = typeArgsOf(d.returnTpt.tpe)
      typeArgs.headOption match {
        case Some(t) if t =:= retElementType => true
        case _                               => false
      }
    } else {
      false
    }
  }

  private def hasStringUnapply(t: TypeRepr): Boolean = {
    t.typeSymbol.companionClass match {
      case cp: Symbol =>
        cp.memberMethod("unapply").headOption.map(_.tree) match {
          case Some(m: DefDef) if m.paramss.size == 1 && hasOptionReturnType(m, t) =>
            val args: List[ParamClause] = m.paramss
            args.headOption.flatMap(_.params.headOption) match {
              // Is the first argument type String? def unapply(s: String)
              case Some(v: ValDef) if v.tpt.tpe =:= TypeRepr.of[String] =>
                true
              case _ =>
                false
            }
          case _ =>
            false
        }
      case _ =>
        false
    }
  }

  private case class MethodArg(name: String, tpe: TypeRepr)

  private def methodArgsOf(t: TypeRepr, method: Symbol): List[MethodArg] = {
    val classTypeParams: List[TypeRepr] = t match {
      case a: AppliedType =>
        a.args
      case _ =>
        List.empty[TypeRepr]
    }

    // println(s"==== method args of ${fullTypeNameOf(t)}")
    method.paramSymss match {
      case List(tpeArgs, methodArgs) =>
        // Resolve type parameters, e.g., class MyClass[A, B]  -> Map("A" -> TypeRepr, "B" -> TypeRepr)
        val typeArgTable = tpeArgs
          .map(_.tree).zipWithIndex.collect {
            case (td: TypeDef, i: Int) if i < classTypeParams.size =>
              td.name -> classTypeParams(i)
          }.toMap[String, TypeRepr]
        // println(s"type args: ${typeArgTable}")
        // tpeArgs for case fields, methodArgs for method arguments
        // E.g. case class Foo(a: String)(implicit b: Int)
        (tpeArgs ++ methodArgs).map(_.tree).collect { case v: ValDef =>
          // Substitue type param to actual types
          val resolved: TypeRepr = v.tpt.tpe match {
            case a: AppliedType =>
              val resolvedTypeArgs = a.args.map {
                case p if p.typeSymbol.isTypeParam && typeArgTable.contains(p.typeSymbol.name) =>
                  typeArgTable(p.typeSymbol.name)
                case other => other
              }
              a.appliedTo(resolvedTypeArgs)
            case other => other
          }
          MethodArg(v.name, resolved)
        }
      case lst =>
        lst.flatten.map(_.tree).collect { case v: ValDef =>
          MethodArg(v.name, v.tpt.tpe)
        }
    }
  }

  private def constructorParametersOf(t: TypeRepr): Expr[Seq[MethodParameter]] = {
    methodParametersOf(t, t.typeSymbol.primaryConstructor)
  }

  private def methodParametersOf(t: TypeRepr, method: Symbol): Expr[Seq[MethodParameter]] = {
    val methodName = method.name
    val methodArgs = methodArgsOf(t, method)
    val argClasses = methodArgs.map { arg =>
      clsOf(arg.tpe.dealias)
    }
    val isConstructor = t.typeSymbol.primaryConstructor == method
    val constructorRef = '{
      MethodRef(
        owner = ${ clsOf(t) },
        name = ${ Expr(methodName) },
        paramTypes = ${ Expr.ofSeq(argClasses) },
        isConstructor = ${ Expr(isConstructor) }
      )
    }

    // println(s"======= ${t.typeSymbol.memberMethods}")

    val paramExprs = for ((field, i) <- methodArgs.zipWithIndex) yield {
      val paramType = field.tpe
      val paramName = field.name
      // println(s"${paramName}: ${v.tpt.show} ${TypeRepr.of[Option[String]].show}")
      // TODO: Use StdMethodParameter when supportin Scala.js in Scala 3
      '{
        wvlet.airframe.surface.reflect.RuntimeMethodParameter(
          method = ${ constructorRef },
          index = ${ Expr(i) },
          name = ${ Expr(paramName) },
          surface = ${ surfaceOf(paramType) }
        )
      }
    }
    Expr.ofSeq(paramExprs)
  }

  private def getTree(e: Expr[_]): Tree = {
    val f = e.getClass().getDeclaredField("tree")
    f.setAccessible(true)
    val tree = f.get(e)
    tree.asInstanceOf[Tree]
  }

  def methodsOf(t: Type[_]): Expr[Seq[MethodSurface]] = {
    methodsOf(TypeRepr.of(using t))
  }

  private def methodsOf(t: TypeRepr): Expr[Seq[MethodSurface]] = {
    val localMethods = localMethodsOf(t).distinct

    val methodSurfaces = localMethods.map(m => (m, m.tree)).collect { case (m, df: DefDef) =>
      val mod   = Expr(modifierBitMaskOf(m))
      val owner = surfaceOf(t)
      val name  = Expr(m.name)
      // println(s"======= ${df.returnTpt.show}")
      val ret = surfaceOf(df.returnTpt.tpe)
      // println(s"==== method of: ${ret.show}")
      val args = methodParametersOf(t, m)
      // TODO: This code doesn't work for Scala.js + Scala 3.0.0
      '{
        wvlet.airframe.surface.reflect
          .ReflectMethodSurface(${ mod }, ${ owner }, ${ name }, ${ ret }, ${ args }.toIndexedSeq)
      }
    }
    Expr.ofSeq(methodSurfaces)
  }

  private def localMethodsOf(t: TypeRepr): Seq[Symbol] = {
    def allMethods = {
      t.typeSymbol.memberMethods
        .filter { x =>
          nonObject(x.owner) &&
          x.isDefDef &&
          // x.isPublic &&
          !x.flags.is(Flags.Private) &&
          !x.flags.is(Flags.Protected) &&
          !x.flags.is(Flags.PrivateLocal) &&
          !x.isClassConstructor &&
          !x.flags.is(Flags.Artifact) &&
          !x.flags.is(Flags.Synthetic) &&
          !x.flags.is(Flags.Macro) &&
          !x.flags.is(Flags.Implicit) &&
          !x.flags.is(Flags.FieldAccessor)
        }
        .filter { x =>
          val name = x.name
          !name.startsWith("$") &&
          name != "<init>"
        }
    }

    allMethods.filter(m => isOwnedByTargetClass(m, t))
  }

  private def nonObject(x: Symbol): Boolean = {
    !x.flags.is(Flags.Synthetic) &&
    !x.flags.is(Flags.Artifact) &&
    x.fullName != "scala.Any" &&
    x.fullName != "java.lang.Object"
  }

  private def isOwnedByTargetClass(m: Symbol, t: TypeRepr): Boolean = {
    m.owner == t.typeSymbol || t.baseClasses.filter(nonObject).exists(_ == m.owner)
  }

  private def modifierBitMaskOf(m: Symbol): Int = {
    var mod = 0

    if (!m.flags.is(Flags.Private) && !m.flags.is(Flags.Protected) && !m.flags.is(Flags.PrivateLocal)) {
      mod |= MethodModifier.PUBLIC
    }
    if (m.flags.is(Flags.Private)) {
      mod |= MethodModifier.PRIVATE
    }
    if (m.flags.is(Flags.Protected)) {
      mod |= MethodModifier.PROTECTED
    }
    if (m.flags.is(Flags.Static)) {
      mod |= MethodModifier.STATIC
    }
    if (m.flags.is(Flags.Final)) {
      mod |= MethodModifier.FINAL
    }
    if (m.flags.is(Flags.Abstract)) {
      mod |= MethodModifier.ABSTRACT
    }
    mod
  }

}
