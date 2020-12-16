package wvlet.airframe.surface
import scala.quoted._

object CompileTimeSurfaceFactory {
    
  type SurfaceMatcher = PartialFunction[Type[_], Expr[Surface]]

  def surfaceOf[A](using tpe: Type[A], quoted: Quotes): Expr[Surface] = {
    import quoted._


    def primitiveFactory: SurfaceMatcher = {
      case t if t == Type.of[String] => '{ Primitive.String }
      case t if t == Type.of[Boolean] => '{ Primitive.Boolean }
      case t if t == Type.of[Int] => '{ Primitive.Int }
      case t if t == Type.of[Long] => '{ Primitive.Long }
      case t if t == Type.of[Float] => '{ Primitive.Float }
      case t if t == Type.of[Double] => '{ Primitive.Double }
      case t if t == Type.of[Short] => '{ Primitive.Short }
      case t if t == Type.of[Byte] => '{ Primitive.Byte }
      case t if t == Type.of[Char] => '{ Primitive.Char }
      case t if t == Type.of[Unit] => '{ Primitive.Unit }
    }

    


    val nullFactory: Expr[Surface] = '{null}

    primitiveFactory.applyOrElse(tpe, { t => nullFactory })
  }


    
}
