package wvlet.airframe.surface
import scala.quoted._

object CompileTimeSurfaceFactory {
    
  type SurfaceMatcher = PartialFunction[Type[_], Expr[Surface]]

  def surfaceOf[A](using tpe: Type[A], quotes: Quotes): Expr[Surface] = {
    import quotes._
    import quotes.reflect._

    val nullFactory: Expr[Surface] = '{null}
    println(Type.show[A])
    println(TypeTree.of(using tpe))


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
      case '[Seq[et]] => '{ new GenericSurface(classOf[Unit]) }
      case _ => nullFactory
    }
  }



}
