package wvlet.airframe.surface
import scala.quoted._

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
        // For inner-class definitions
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
      // println(s"[${typeNameOf(t)}]\n  ${t}\nfull type name: ${fullTypeNameOf(t)}\nclass: ${t.getClass}")
      val generator = factory.andThen { expr =>
        val cacheKey = if (typeNameOf(t) == "scala.Any") {
          t match {
            case ParamRef(TypeLambda(typeNames, _, _), _) =>
              // Distinguish scala.Any and type bounds (such as _)
              s"${fullTypeNameOf(t)} for ${t}"
            case TypeBounds(_, _) =>
              // This ensures different cache key for each Type Parameter (such as T and U).
              // This is required because fullTypeNameOf of every Type Parameters is `scala.Any`.
              s"${fullTypeNameOf(t)} for ${t}"
            case _ =>
              fullTypeNameOf(t)
          }
        } else {
          fullTypeNameOf(t)
        }
        '{
          val key = ${ Expr(cacheKey) }
          if (!wvlet.airframe.surface.surfaceCache.contains(key)) {
            wvlet.airframe.surface.surfaceCache += key -> ${ expr }
          }
          wvlet.airframe.surface.surfaceCache(key)
        }
      }
      val surface = generator(t)
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
      // println(s"=== alias factory: ${t}, ${dealiased}, ${t.simplified}")
      val symbolInOwner = t.typeSymbol.maybeOwner.declarations.find(_.name.toString == t.typeSymbol.name.toString)
      val inner = symbolInOwner.map(_.tree) match {
        case Some(TypeDef(_, b: TypeTree)) if t == dealiased =>
          // t.dealias does not dealias for path dependent types, so extracting the dealiased type from AST.
          surfaceOf(b.tpe)
        case _ =>
          if (t != dealiased) {
            surfaceOf(dealiased)
          } else {
            surfaceOf(t.simplified)
          }
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
    case p @ ParamRef(TypeLambda(typeNames, _, _), _) =>
      val name     = typeNames.mkString(",")
      val fullName = fullTypeNameOf(p)
      '{ HigherKindedTypeSurface(${ Expr(name) }, ${ Expr(fullName) }, AnyRefSurface, Seq.empty) }
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
    typeArgsOf(t).map(surfaceOf(_)).headOption match {
      case Some(expr) =>
        expr
      case None =>
        // FIXME: Is this right ?
        '{ AnyRefSurface }
    }
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
          && Option(t.typeSymbol.primaryConstructor)
            .exists(p => p.exists && !p.flags.is(Flags.Private) && p.paramSymss.nonEmpty) =>
      val typeArgs     = typeArgsOf(t.simplified).map(surfaceOf(_))
      val methodParams = constructorParametersOf(t)
      // val isStatic     = !t.typeSymbol.flags.is(Flags.Local)
      val factory = createObjectFactoryOf(t) match {
        case Some(x) => '{ Some(${ x }) }
        case None    => '{ None }
      }

      '{
        new wvlet.airframe.surface.GenericSurface(
          ${ clsOf(t) },
          ${ Expr.ofSeq(typeArgs) }.toIndexedSeq,
          params = ${ methodParams },
          objectFactory = ${ factory }
        )
      }
  }

  private def typeMappingTable(t: TypeRepr, method: Symbol): Map[String, TypeRepr] = {
    val classTypeParams: List[TypeRepr] = t match {
      case a: AppliedType => a.args
      case _              => List.empty[TypeRepr]
    }

    // Build a table for resolving type parameters, e.g., class MyClass[A, B]  -> Map("A" -> TypeRepr, "B" -> TypeRepr)
    method.paramSymss match {
      // tpeArgs for case fields, methodArgs for method arguments
      case tpeArgs :: tail if t.typeSymbol.typeMembers.nonEmpty =>
        val typeArgTable = tpeArgs
          .map(_.tree).zipWithIndex.collect {
            case (td: TypeDef, i: Int) if i < classTypeParams.size =>
              td.name -> classTypeParams(i)
          }.toMap[String, TypeRepr]
        // pri ntln(s"type args: ${typeArgTable}")
        typeArgTable
      case _ =>
        Map.empty
    }
  }

  // Get a constructor with its generic types are resolved
  private def getResolvedConstructorOf(t: TypeRepr): Option[Term] = {
    val ts = t.typeSymbol
    ts.primaryConstructor match {
      case pc if pc == Symbol.noSymbol =>
        None
      case pc =>
        // val cstr = Select.apply(New(TypeIdent(ts)), "<init>")
        val cstr = New(Inferred(t)).select(pc)
        if (ts.typeMembers.isEmpty) {
          Some(cstr)
        } else {
          val lookupTable = typeMappingTable(t, pc)
          // println(s"--- ${lookupTable}")
          val typeArgs = pc.paramSymss.headOption.getOrElse(List.empty).map(_.tree).collect { case t: TypeDef =>
            lookupTable.getOrElse(t.name, TypeRepr.of[AnyRef])
          }
          Some(cstr.appliedToTypes(typeArgs))
        }
    }
  }

  private def createObjectFactoryOf(targetType: TypeRepr): Option[Expr[ObjectFactory]] = {
    val ts    = targetType.typeSymbol
    val flags = ts.flags
    if (
      flags.is(Flags.Abstract) || flags.is(Flags.Module) || hasAbstractMethods(targetType) || isPathDependentType(
        targetType
      )
    ) {
      None
    } else {
      getResolvedConstructorOf(targetType).map { cstr =>
        val argListList = methodArgsOf(targetType, ts.primaryConstructor)
        val newClassFn = Lambda(
          owner = Symbol.spliceOwner,
          tpe = MethodType(List("args"))(_ => List(TypeRepr.of[Seq[Any]]), _ => TypeRepr.of[Any]),
          rhsFn = (sym: Symbol, paramRefs: List[Tree]) => {
            val args  = paramRefs.head.asExprOf[Seq[Any]].asTerm
            var index = 0
            val fn = argListList.foldLeft[Term](cstr) { (prev, argList) =>
              val argExtractors = argList.map { a =>
                // args(i+1)
                val extracted = Select.unique(args, "apply").appliedTo(Literal(IntConstant(index)))
                index += 1
                // args(i+1).asInstanceOf[A]
                // TODO: Cast primitive values to target types
                Select.unique(extracted, "asInstanceOf").appliedToType(a.tpe)
              }
              Apply(prev, argExtractors.toList)
            }
            // println(s"== ${fn.show}")
            fn.changeOwner(sym)
          }
        )
        val expr = '{
          new wvlet.airframe.surface.ObjectFactory {
            override def newInstance(args: Seq[Any]): Any = { ${ newClassFn.asExprOf[Seq[Any] => Any] }(args) }
          }
        }
        expr
      }
    }
  }

  private def hasAbstractMethods(t: TypeRepr): Boolean = {
    t.typeSymbol.methodMembers.exists(_.flags.is(Flags.Abstract))
  }

  private def isPathDependentType(t: TypeRepr): Boolean = {
    !t.typeSymbol.flags.is(Flags.Static) && (t match {
      case t: TypeBounds => true
      case _             => false
    })
  }

  private def genericTypeFactory: Factory = {
    case a: AppliedType =>
      val typeArgs = a.args.map(surfaceOf(_))
      '{ new GenericSurface(${ clsOf(a) }, typeArgs = ${ Expr.ofSeq(typeArgs) }.toIndexedSeq) }
    // special treatment for type Foo = Foo[Buz]
    case TypeBounds(a1: AppliedType, a2: AppliedType) if a1 == a2 =>
      val typeArgs = a1.args.map(surfaceOf(_))
      '{ new GenericSurface(${ clsOf(a1) }, typeArgs = ${ Expr.ofSeq(typeArgs) }.toIndexedSeq) }
    case r: Refinement =>
      newGenericSurfaceOf(r.info)
    case t if hasStringUnapply(t) =>
      // Build EnumSurface.apply code
      // EnumSurface(classOf[t], { (cl: Class[_], s: String) => (companion object).unapply(s).asInstanceOf[Option[Any]] }
      val unapplyMethod = getStringUnapply(t).get
      val m             = Ref(t.typeSymbol.companionModule).select(unapplyMethod)
      val newFn = Lambda(
        owner = Symbol.spliceOwner,
        tpe = MethodType(List("cl", "s"))(
          _ => List(TypeRepr.of[Class[_]], TypeRepr.of[String]),
          _ => TypeRepr.of[Option[Any]]
        ),
        rhsFn = (sym: Symbol, paramRefs: List[Tree]) => {
          val strVarRef = paramRefs(1).asExprOf[String].asTerm
          Select.unique(Apply(m, List(strVarRef)), "asInstanceOf").appliedToType(TypeRepr.of[Option[Any]])
        }
      )
      '{
        EnumSurface(
          ${ clsOf(t) },
          ${ newFn.asExprOf[(Class[_], String) => Option[Any]] }
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
    getStringUnapply(t).isDefined
  }

  private def getStringUnapply(t: TypeRepr): Option[Symbol] = {
    t.typeSymbol.companionClass match {
      case cp: Symbol =>
        val methodOpt = cp.methodMember("unapply").headOption
        methodOpt.map(_.tree) match {
          case Some(m: DefDef) if m.paramss.size == 1 && hasOptionReturnType(m, t) =>
            val args: List[ParamClause] = m.paramss
            args.headOption.flatMap(_.params.headOption) match {
              // Is the first argument type String? def unapply(s: String)
              case Some(v: ValDef) if v.tpt.tpe =:= TypeRepr.of[String] =>
                methodOpt
              case _ =>
                None
            }
          case _ =>
            None
        }
      case null =>
        None
    }
  }

  private case class MethodArg(
      name: String,
      tpe: TypeRepr,
      defaultValueGetter: Option[Symbol],
      defaultMethodArgGetter: Option[Symbol],
      isImplicit: Boolean,
      isRequired: Boolean,
      isSecret: Boolean
  )

  private def methodArgsOf(t: TypeRepr, method: Symbol): List[List[MethodArg]] = {
    // println(s"==== method args of ${fullTypeNameOf(t)}")

    val defaultValueMethods = t.typeSymbol.companionClass.declaredMethods.filter { m =>
      m.name.startsWith("apply$default$") || m.name.startsWith("$lessinit$greater$default$")
    }

    // Build a table for resolving type parameters, e.g., class MyClass[A, B]  -> Map("A" -> TypeRepr, "B" -> TypeRepr)
    val typeArgTable: Map[String, TypeRepr] = typeMappingTable(t, method)

    val origParamSymss = method.paramSymss
    val paramss =
      if (origParamSymss.nonEmpty && t.typeSymbol.typeMembers.nonEmpty) origParamSymss.tail else origParamSymss

    paramss.map { params =>
      params.zipWithIndex
        .map((x, i) => (x, i + 1, x.tree))
        .collect { case (s: Symbol, i: Int, v: ValDef) =>
          // E.g. case class Foo(a: String)(implicit b: Int)
          // println(s"=== ${v.show} ${s.flags.show} ${s.flags.is(Flags.Implicit)}")
          // Substitue type param to actual types
          val resolved: TypeRepr = v.tpt.tpe match {
            case a: AppliedType =>
              val resolvedTypeArgs = a.args.map {
                case p if p.typeSymbol.isTypeParam && typeArgTable.contains(p.typeSymbol.name) =>
                  typeArgTable(p.typeSymbol.name)
                case other =>
                  other
              }
              a.appliedTo(resolvedTypeArgs)
            case TypeRef(_, name) if typeArgTable.contains(name) =>
              typeArgTable(name)
            case other =>
              other
          }
          val isSecret           = hasSecretAnnotation(s)
          val isRequired         = hasRequiredAnnotation(s)
          val isImplicit         = s.flags.is(Flags.Implicit)
          val defaultValueGetter = defaultValueMethods.find(m => m.name.endsWith(s"$$${i}"))

          val defaultMethodArgGetter = {
            val targetMethodName = method.name + "$default$" + i
            t.typeSymbol.declaredMethods.find { m =>
              // println(s"=== target: ${m.name}, ${m.owner.name}")
              m.name == targetMethodName
            }
          }
          MethodArg(v.name, resolved, defaultValueGetter, defaultMethodArgGetter, isImplicit, isRequired, isSecret)
        }
    }
  }

  private def hasSecretAnnotation(s: Symbol): Boolean = {
    val t = TypeRepr.of[wvlet.airframe.surface.secret]
    s.hasAnnotation(t.typeSymbol)
  }
  private def hasRequiredAnnotation(s: Symbol): Boolean = {
    val t = TypeRepr.of[wvlet.airframe.surface.required]
    s.hasAnnotation(t.typeSymbol)
  }

  private def constructorParametersOf(t: TypeRepr): Expr[Seq[MethodParameter]] = {
    methodParametersOf(t, t.typeSymbol.primaryConstructor)
  }

  private def methodParametersOf(t: TypeRepr, method: Symbol): Expr[Seq[MethodParameter]] = {
    val methodName = method.name
    val methodArgs = methodArgsOf(t, method).flatten
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

      // Related example:
      // https://github.com/lampepfl/dotty-macro-examples/blob/aed51833db652f67741089721765ad5a349f7383/defaultParamsInference/src/macro.scala
      val defaultValue: Expr[Option[Any]] = field.defaultValueGetter match {
        case Some(m) =>
          val dv = Ref(m.owner.companionModule).select(m)
          '{ Some(${ dv.asExprOf[Any] }) }
        case _ => '{ None }
      }

      // Generate a field accessor { (x:Any) => x.asInstanceOf[A].(field name) }
      val paramIsAccessible = {
        t.typeSymbol.fieldMember(paramName) match {
          case nt if nt == Symbol.noSymbol     => false
          case m if m.flags.is(Flags.Private)  => false
          case m if m.flags.is(Flags.Artifact) => false
          case _                               => true
        }
      }
      // println(s"${paramName} ${paramIsAccessible}")

      val accessor: Expr[Option[Any => Any]] = if (method.isClassConstructor && paramIsAccessible) {
        val lambda = Lambda(
          owner = Symbol.spliceOwner,
          tpe = MethodType(List("x"))(_ => List(TypeRepr.of[Any]), _ => TypeRepr.of[Any]),
          rhsFn = (sym, params) => {
            val x    = params.head.asInstanceOf[Term]
            val expr = Select.unique(Select.unique(x, "asInstanceOf").appliedToType(t), paramName)
            expr.changeOwner(sym)
          }
        )
        // println(t.typeSymbol)
        // println(paramType.typeSymbol.flags.show)
        // println(lambda.show)
        // println(lambda.show(using Printer.TreeStructure))
        '{ Some(${ lambda.asExprOf[Any => Any] }) }
      } else {
        '{ None }
      }

      val methodArgAccessor: Expr[Option[Any => Any]] = field.defaultMethodArgGetter match {
        case None =>
          '{ None }
        case Some(m) =>
          val lambda = Lambda(
            owner = Symbol.spliceOwner,
            tpe = MethodType(List("x"))(_ => List(TypeRepr.of[Any]), _ => TypeRepr.of[Any]),
            rhsFn = (sym, params) => {
              val x    = params.head.asInstanceOf[Term]
              val expr = Select.unique(x, "asInstanceOf").appliedToType(t).select(m)
              expr.changeOwner(sym)
            }
          )
          '{ Some(${ lambda.asExprOf[Any => Any] }) }
      }

      // Using StaticMethodParameter when supportin Scala.js in Scala 3.
      // TODO: Deprecate RuntimeMethodParameter
      '{
        wvlet.airframe.surface.StaticMethodParameter(
          method = ${ constructorRef },
          index = ${ Expr(i) },
          name = ${ Expr(paramName) },
          isRequired = ${ Expr(field.isRequired) },
          isSecret = ${ Expr(field.isSecret) },
          surface = ${ surfaceOf(paramType) },
          defaultValue = ${ defaultValue },
          accessor = ${ accessor },
          methodArgAccessor = ${ methodArgAccessor }
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

  private val methodMemo = scala.collection.mutable.Map[TypeRepr, Expr[Seq[MethodSurface]]]()
  private val methodSeen = scala.collection.mutable.Set[TypeRepr]()

  private def methodsOf(targetType: TypeRepr): Expr[Seq[MethodSurface]] = {
    if (methodMemo.contains(targetType)) methodMemo(targetType)
    else if (seen.contains(targetType)) {
      sys.error(s"recurcive type in method: ${targetType.typeSymbol.fullName}")
    } else {
      methodSeen += targetType
      val localMethods = localMethodsOf(targetType).distinct
      val methodSurfaces = localMethods.map(m => (m, m.tree)).collect { case (m, df: DefDef) =>
        val mod   = Expr(modifierBitMaskOf(m))
        val owner = surfaceOf(targetType)
        val name  = Expr(m.name)
        // println(s"======= ${df.returnTpt.show}")
        val ret = surfaceOf(df.returnTpt.tpe)
        // println(s"==== method of: def ${m.name}")
        val params       = methodParametersOf(targetType, m)
        val args         = methodArgsOf(targetType, m).flatten
        val methodCaller = createMethodCaller(targetType, m, args)
        '{
          ClassMethodSurface(${ mod }, ${ owner }, ${ name }, ${ ret }, ${ params }.toIndexedSeq, ${ methodCaller })
        }
      }
      val expr = Expr.ofSeq(methodSurfaces)
      methodMemo += targetType -> expr
      expr
    }
  }

  private def isTypeParam(t: TypeRepr): Boolean = {
    t match {
      case TypeRef(prefix, typeName) if prefix.toString == "NoPrefix" => true
      case _                                                          => false
    }
  }

  private def createMethodCaller(
      objectType: TypeRepr,
      m: Symbol,
      methodArgs: Seq[MethodArg]
  ): Expr[Option[(Any, Seq[Any]) => Any]] = {
    // Build { (x: Any, args: Seq[Any]) => x.asInstanceOf[t].<method>(.. args) }
    val methodTypeParams: List[TypeParamClause] = m.tree match {
      case df: DefDef =>
        df.paramss.collect { case t: TypeParamClause =>
          t
        }
      case _ =>
        List.empty
    }

    val lambda = Lambda(
      owner = Symbol.spliceOwner,
      tpe = MethodType(List("x", "args"))(_ => List(TypeRepr.of[Any], TypeRepr.of[Seq[Any]]), _ => TypeRepr.of[Any]),
      rhsFn = (sym, params) => {
        val x    = params(0).asInstanceOf[Term]
        val args = params(1).asInstanceOf[Term]
        val expr = Select.unique(x, "asInstanceOf").appliedToType(objectType).select(m)
        val argList = methodArgs.zipWithIndex.collect {
          // If the arg is implicit, no need to explicitly bind it
          case (arg, i) if !arg.isImplicit =>
            // args(i).asInstanceOf[ArgType]
            val extracted = Select.unique(args, "apply").appliedTo(Literal(IntConstant(i)))
            Select.unique(extracted, "asInstanceOf").appliedToType(arg.tpe)
        }
        if (argList.isEmpty) {
          val newExpr = m.tree match {
            case d: DefDef if d.trailingParamss.nonEmpty =>
              // An empty arg method, e.g., def methodName()
              expr.appliedToNone
            case _ =>
              // No arg method, e.g., def methodName: Unit
              expr
          }
          newExpr.changeOwner(sym)
        } else {
          // Bind to function arguments
          val newExpr = if (methodTypeParams.isEmpty) {
            expr.appliedToArgs(argList.toList)
          } else {
            // For generic functions, type params also need to be applied
            val dummyTypeParams = methodTypeParams.map(x => TypeRepr.of[Any])
            // println(s"---> ${m.name} type param count: ${methodTypeParams.size}, arg size: ${argList.size}")
            expr
              .appliedToTypes(dummyTypeParams)
              .appliedToArgs(argList.toList)
          }
          newExpr.changeOwner(sym)
        }
      }
    )
    '{ Some(${ lambda.asExprOf[(Any, Seq[Any]) => Any] }) }
  }

  private def localMethodsOf(t: TypeRepr): Seq[Symbol] = {
    def allMethods = {
      t.typeSymbol.methodMembers
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
    x.fullName != "java.lang.Object" &&
    // Exclude caes class methods
    x.fullName != "scala.Product"
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

  def surfaceFromClass(cl: Class[_]): Expr[Surface] = {
    val name         = cl.getName
    val rawType      = Class.forName(name)
    val constructors = rawType.getConstructors
    val (typeArgs, params) = if (constructors.nonEmpty) {
      val primaryConstructor = constructors(0)
      val paramSurfaces: Seq[Expr[Surface]] = primaryConstructor.getParameterTypes.map { paramType =>
        val tastyType = quotes.reflect.TypeRepr.typeConstructorOf(paramType)
        surfaceOf(tastyType)
      }.toSeq
      // FIXME: Use TastyInspector as runtime-like reflection for Scala 3
      val params: Seq[Expr[Parameter]] = Seq.empty
      (Expr.ofSeq(paramSurfaces), Expr.ofSeq(params))
    } else {
      ('{ Seq.empty[Surface] }, '{ Seq.empty[Parameter] })
    }

    '{
      new wvlet.airframe.surface.GenericSurface(
        rawType = Class.forName(${ Expr(name) }),
        typeArgs = ${ typeArgs }
      )
    }
  }

}
