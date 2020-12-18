package wvlet.airframe.surface
import scala.quoted._

object CompileTimeSurfaceFactory {

  type SurfaceMatcher = PartialFunction[Type[_], Expr[Surface]]


  def surfaceOf[A](using tpe: Type[A], quotes: Quotes): Expr[Surface] = {
    import quotes._
    import quotes.reflect._

    val f = new CompileTimeSurfaceFactory(using quotes)
    f.surfaceOf(tpe)

    // def findPrimaryConstructorOf(t: Type[_]): Option[Symbol] = {
    //    val r = TypeRepr.of(using t)
    //    val pc = r.typeSymbol.primaryConstructor
    //    if(pc.exists) {
    //     println(pc.paramSymss.mkString(", "))
    //    }
    //    None
    //    //.filter(m => m.isClassConstructor && m.isPublic).map(_.asMethod)
    // }

    // val pc = findPrimaryConstructorOf(tpe)
    // println
    //val ex = Expr(classOf[Int])
    //println(Term.of(ex))
    //ex.show(using Printer.TreeCode)

    //   case '[Seq[elementType]] => 
    //   { 
    //       val tt = TypeTree.of[A]
    //       val clsOf = Literal(ClassOfConstant(tt.tpe)).asExpr.asInstanceOf[Expr[Class[A]]]
    //       '{ new GenericSurface(${clsOf}) }
    //   }
    //   case _ => nullFactory
    // }
  }

}

class CompileTimeSurfaceFactory(using quotes:Quotes) {
  import quotes._
  import quotes.reflect._


  private def fullTypeNameOf(t:Type[_]): String = {
     fullTypeNameOf(TypeRepr.of(using t))
  }

  private def fullTypeNameOf(t:TypeRepr): String = {
      def sanitize(symbol:Symbol): String = {
        val fullName = symbol.fullName
        fullName.split("\\.").toList match {
          case "scala" :: "Predef$" :: tail =>
            tail.mkString(".")
          case "scala" :: "collection" :: "immutable" :: tail =>
            tail.mkString(".")
          case "scala" :: nme :: Nil =>
            nme
          case _ => 
            fullName.replaceAll("\\$", "")
        }
      }
      t match {
        case a:AppliedType if a.args.nonEmpty =>
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
    if(seen.contains(t)) {
      if(memo.contains(t)) {
        memo(t)
      }
      else {
        '{ LazySurface(${clsOf(t)}, ${Expr(fullTypeNameOf(t))}) }
      }
    }
    else {
      seen += t
      // For debugging
      // println(s"[${typeNameOf(t)}]\n  ${t}")
      val generator = factory.andThen { expr =>
        '{ wvlet.airframe.surface.surfaceCache.getOrElseUpdate(${Expr(fullTypeNameOf(t))}, ${expr}) }
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
    caseClassFactory orElse
    genericTypeFactory
  }

  private def primitiveTypeFactory: Factory = {
    case t if t =:= TypeRepr.of[String] => '{ Primitive.String }
    case t if t =:= TypeRepr.of[Boolean] => '{ Primitive.Boolean }
    case t if t =:= TypeRepr.of[Int] => '{ Primitive.Int }
    case t if t =:= TypeRepr.of[Long] => '{ Primitive.Long }
    case t if t =:= TypeRepr.of[Float] => '{ Primitive.Float }
    case t if t =:= TypeRepr.of[Double] => '{ Primitive.Double }
    case t if t =:= TypeRepr.of[Short] => '{ Primitive.Short }
    case t if t =:= TypeRepr.of[Byte] => '{ Primitive.Byte }
    case t if t =:= TypeRepr.of[Char] => '{ Primitive.Char }
    case t if t =:= TypeRepr.of[Unit] => '{ Primitive.Unit }
  }

  private def typeNameOf(t: TypeRepr): String = {
    t.typeSymbol.fullName
  }

  private def isTaggedType(t: TypeRepr): Boolean = {
    typeNameOf(t).startsWith("wvlet.airframe.surface.tag.")
  }

  private def taggedTypeFactory: Factory = {
    case a : AppliedType if a.args.length == 2 && isTaggedType(a) =>
      '{ TaggedSurface(${surfaceOf(a.args(0))}, ${surfaceOf(a.args(1))}) }
  }

  private def belongsToScalaDefault(t: TypeRepr): Boolean = {
    val scalaDefaultPackages = Seq("scala.", "scala.Predef$.", "scala.util.")
    val nme = t.typeSymbol.fullName
    scalaDefaultPackages.exists(p => nme.startsWith(p))
  }

  private def aliasFactory: Factory = {
    case t if t.typeSymbol.isType && t.typeSymbol.isAliasType && !belongsToScalaDefault(t) =>
      val dealiased = t.dealias
      val inner = if(t != dealiased) {
        surfaceOf(dealiased)
      }
      else {
        surfaceOf(t.simplified)
      }
      val s = t.typeSymbol
      val name = Expr(s.name)
      val fullName = Expr(fullTypeNameOf(t.asType))
      '{ Alias(${name}, ${fullName}, ${inner}) } 
  }

  private def higherKindedTypeFactory: Factory = {
    case h: TypeLambda => 
      val name = h.typeSymbol.name
      val fullName = fullTypeNameOf(h)
      val inner = surfaceOf(h.resType)

      val len = h.paramNames.size
      val params = (0 until len).map{ i => h.param(i) }
      val args = params.map(surfaceOf(_))
      '{ HigherKindedTypeSurface(${Expr(name)}, ${Expr(fullName)}, ${inner}, ${Expr.ofSeq(args)} ) }
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
      '{ ArraySurface(${clsOf(t)}, ${elementTypeSurfaceOf(t)}) }
  }

  private def optionFactory: Factory = {
    case t if typeNameOf(t) == "scala.Option" =>
      '{ OptionSurface(${clsOf(t)}, ${elementTypeSurfaceOf(t)})}
  }

  private def tupleFactory: Factory = {
    case t if t <:< TypeRepr.of[Product] && typeNameOf(t).startsWith("scala.Tuple") =>
      val paramTypes = typeArgsOf(t).map(surfaceOf(_))
      '{ new TupleSurface(${clsOf(t)}, ${Expr.ofSeq(paramTypes)}.toIndexedSeq) }
  }

  private def javaUtilFactory: Factory = {
    // For common Java classes, stop with this rule so as not to extract internal parameters
    case t if t =:= TypeRepr.of[java.io.File] || 
      t =:= TypeRepr.of[java.util.Date] || 
      t =:= TypeRepr.of[java.time.temporal.Temporal] =>
     newGenericSurfaceOf(t) 
  }

  private def isEnum(t:TypeRepr): Boolean = {
    t.baseClasses.exists(x => x.fullName.startsWith("java.lang.Enum"))
  }

  private def javaEnumFactory: Factory = {
    case t if isEnum(t) =>
      '{ JavaEnumSurface(${clsOf(t)}) }
  }

  private def exisitentialTypeFactory: Factory = {
     case t : TypeBounds if t.hi =:= TypeRepr.of[Any] =>
      // TODO Represent low/hi bounds
      '{ ExistentialType }  
  }

  private def clsOf(t:TypeRepr): Expr[Class[_]] = {
    Literal(ClassOfConstant(t)).asExpr.asInstanceOf[Expr[Class[_]]]
  }

  private def newGenericSurfaceOf(t:TypeRepr): Expr[Surface] = {
    '{ new GenericSurface(${clsOf(t)}) }
  }

  private def caseClassFactory: Factory = {
    case t if t.typeSymbol.caseFields.nonEmpty =>
      val typeArgs = typeArgsOf(t).map(surfaceOf(_))
      val params = caseParametersOf(t)
      '{ new GenericSurface(
           ${clsOf(t)}, 
           ${Expr.ofSeq(typeArgs)}.toIndexedSeq,
           params = ${params}
         )
      }
  }

  private def genericTypeFactory: Factory = {
    case a: AppliedType =>
      val typeArgs = a.args.map(surfaceOf(_))
      '{ new GenericSurface(${clsOf(a)}, typeArgs = ${Expr.ofSeq(typeArgs)}.toIndexedSeq) }
    case r: Refinement =>
      newGenericSurfaceOf(r.info)
    case t =>
      newGenericSurfaceOf(t)
  }

  private def methodArgsOf(method:Symbol): List[Symbol] = {
    method.paramSymss.flatten
  }

  private def caseParametersOf(t: TypeRepr): Expr[Seq[MethodParameter]] = {
    val cstr = t.typeSymbol.primaryConstructor
    val constructorName = cstr.name
    val argClasses = methodArgsOf(cstr).map(_.tree).collect { 
      case v:ValDef =>
        println(s"${v.name}: ${v}")
        clsOf(v.tpt.tpe.dealias)
    }
    val constructorRef = '{
      MethodRef(owner = ${clsOf(t)}, name = ${Expr(constructorName)}, paramTypes = ${Expr.ofSeq(argClasses)}, isConstructor =true)
    }

    val paramExprs = for{ 
      (field, v:ValDef, i) <- t.typeSymbol.caseFields.zipWithIndex.map((f, i) => (f, f.tree, i))
    } yield {
      val paramType = v.tpt.tpe
      val paramName = field.name
      //println(s"${paramName}: ${paramType}")
      '{
        wvlet.airframe.surface.reflect.RuntimeMethodParameter(
          method = ${constructorRef},
          index = ${Expr(i)},
          name = ${Expr(paramName)},
          surface = ${surfaceOf(paramType)}
        )
      }
    }
    Expr.ofSeq(paramExprs)
  }


}