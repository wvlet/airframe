package wvlet.airframe.surface.reflect

import wvlet.log.LogSupport
import wvlet.airframe.surface.{MethodSurface, Surface, Primitive}
import wvlet.airframe.surface.CompileTimeSurfaceFactory

import java.util.concurrent.ConcurrentHashMap
import scala.quoted._
import scala.tasty.inspector._

object TastySurfaceFactory extends LogSupport {

  given staging.Compiler = staging.Compiler.make(getClass.getClassLoader)

  inline def of[A]: Surface = ${ CompileTimeSurfaceFactory.surfaceOf[A] }

  import scala.jdk.CollectionConverters._
  private val cache = new ConcurrentHashMap[Class[_], Surface]().asScala

  def ofClass(cl: Class[_]): Surface = {
    debug(s"ofClass: ${cl}")
    cache.getOrElseUpdate(
      cl, {
        debug(s"Update cache for ${cl}")
        // Generates Surface from a runtime class
        staging.run { (quotes: Quotes) ?=>
          import quotes.reflect._
          val tastyType = quotes.reflect.TypeRepr.typeConstructorOf(cl)
          debug(tastyType)
          val f = new CompileTimeSurfaceFactory(using quotes)
          tastyType match {
            case t if t.show == "<none>.<none>" =>
              val name = cl.getName
              // FIXME: A workaround for runtime surface generation.
              // Example use case is MessageCodec.of[Any].
              // `GenericSurface(${ Expr(cl) }) `causes `scala.MatchError: NoType` ¯\_(ツ)_/¯
              '{ new wvlet.airframe.surface.GenericSurface(${ Expr(cl) }) }
            case _ =>
              f.surfaceOf(tastyType.asType)
          }
        }
      }
    )
  }

  def methodsOfClass(cl: Class[_]): Seq[MethodSurface] = {
    // Generates Surface from a runtime class
    val code: Seq[MethodSurface] = staging.run { (quotes: Quotes) ?=>
      import quotes.reflect._
      val tastyType = quotes.reflect.TypeRepr.typeConstructorOf(cl)
      debug(tastyType)
      val f = new CompileTimeSurfaceFactory(using quotes)
      f.methodsOf(tastyType.asType)
    }
    code
  }

}
