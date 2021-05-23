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
    info(s"ofClass: ${cl}")
    cache.getOrElseUpdate(cl, {
      info(s"Update cache for ${cl}")
      // Generates Surface from a runtime class
      staging.run {
        (quotes: Quotes) ?=>
        import quotes.reflect._
        val tastyType = quotes.reflect.TypeRepr.typeConstructorOf(cl)
        info(tastyType)
        val f = new CompileTimeSurfaceFactory(using quotes)
        val expr = f.surfaceOf(tastyType.asType)
        expr
      }
    }
    )
  }

  def methodsOfClass(cl: Class[_]): Seq[MethodSurface] = {
    // Generates Surface from a runtime class
    val code: Seq[MethodSurface] = staging.run {
      (quotes: Quotes) ?=>
      import quotes.reflect._
      val tastyType = quotes.reflect.TypeRepr.typeConstructorOf(cl)
      info(tastyType)
      val f = new CompileTimeSurfaceFactory(using quotes)
      f.methodsOf(tastyType.asType)
    }
    code
  }

}
