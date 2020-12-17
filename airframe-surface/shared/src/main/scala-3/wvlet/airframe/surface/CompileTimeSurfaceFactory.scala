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
      val tt = TypeTree.of(using t)
      tt.tpe match {
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

  private def surfaceOf(tr: TypeRepr): Expr[Surface] = {
    val nullFactory: Expr[Surface] = '{null}
    primitiveTypeFactory.applyOrElse(tr, { x => nullFactory } )
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

  
}