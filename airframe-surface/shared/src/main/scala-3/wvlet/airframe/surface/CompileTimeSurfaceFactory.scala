package wvlet.airframe.surface
import scala.quoted._

object CompileTimeSurfaceFactory {

  type SurfaceMatcher = PartialFunction[Type[_], Expr[Surface]]

  def surfaceOf[A](using tpe: Type[A], quotes: Quotes): Expr[Surface] = {
    import quotes._
    import quotes.reflect._

    val nullFactory: Expr[Surface] = '{null}
    //println(Type.show[A])
    //println(TypeTree.of(using tpe))

    def lift[T](using t:Type[T]): Type[T] = {
       t
    }

    tpe match {
      case '[String] => '{ Primitive.String }
      case '[Boolean] => '{ Primitive.Boolean }
      case '[Int] => '{ Primitive.Int }
      case '[Long] => '{ Primitive.Long }
      case '[Float] => '{ Primitive.Float }
      case '[Double] => '{ Primitive.Double }
      case '[Short] => '{ Primitive.Short }
      case '[Byte] => '{ Primitive.Byte }
      case '[Char] => '{ Primitive.Char }
      case '[Unit] => '{ Primitive.Unit }
      case '[Seq[elementType]] => 
      { 
          val t = implicitly[Type[elementType]]
          val tt = TypeRepr.of(using t)
          //val cl = ClassOfConstant(tt)
          val ts = tt.classSymbol.get
          val cl = Constant.ClassOf(tt)
          println(cl)
          //val expr = '{ classOf[t.Underlying] }
          //println(expr.show)
          val e = '{ new GenericSurface(classOf[Unit]) }
          e
      }
      case _ => nullFactory
    }
  }

}
