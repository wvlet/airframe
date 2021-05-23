package wvlet.airframe.surface.reflect

import wvlet.log.LogSupport
import wvlet.airframe.surface.Surface
import wvlet.airframe.surface.LazySurface
import wvlet.airframe.surface.CompileTimeSurfaceFactory

import scala.quoted._
import scala.tasty.inspector._

object TastySurfaceFactory extends LogSupport {

  given staging.Compiler = staging.Compiler.make(getClass.getClassLoader)

  inline def of[A]: Surface = ${ CompileTimeSurfaceFactory.surfaceOf[A] }

  def ofClass(cl: Class[_]): Surface = {
    // Generates Surface from a runtime class
    val surface = staging.run {
      (quotes: Quotes) ?=>
      import quotes.reflect._
      val tastyType = quotes.reflect.TypeRepr.typeConstructorOf(cl)
      val f = new CompileTimeSurfaceFactory(using quotes)
      f.surfaceOf(tastyType.asType)
    }
    surface
  }

}
