package wvlet.airframe.surface
import scala.quoted._

object CompileTimeSurfaceFactory {
       
  def surfaceOf[A](using tpe: Type[A], quoted: Quotes): Expr[Surface] = {
    import quoted._
    println(tpe.toString)
    println(s"hello macros")

    '{ null }
  }
}