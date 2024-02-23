package wvlet.airframe.surface.reflect

import wvlet.log.LogSupport
import wvlet.airframe.surface.AirSpecBridge

case class Person(id: Int, name: String):
  def hello: String = "hello"

sealed trait Status
object Status:
  case object Ok extends Status

class TastySurfaceFactoryTest extends munit.FunSuite with AirSpecBridge with LogSupport:

  test("of[A]") {
    val s = TastySurfaceFactory.of[Person]
    debug(s.params.mkString(", "))
  }

  test("ofClass") {
    val s = ReflectSurfaceFactory.ofClass(classOf[Person])
    debug(s)

    val s2 = ReflectSurfaceFactory.ofClass(classOf[Person])
    debug(s2)
  }

  test("methodsOf") {
    pending("runtime error is shown")
    val m = TastySurfaceFactory.methodsOfClass(classOf[Person])
    debug(m.mkString(", "))
  }

  test("case object") {
    TastySurfaceFactory.ofClass(classOf[Status])
    TastySurfaceFactory.ofClass(classOf[Status.Ok.type])
  }
