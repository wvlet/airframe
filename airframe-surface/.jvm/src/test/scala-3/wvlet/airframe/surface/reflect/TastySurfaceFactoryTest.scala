package wvlet.airframe.surface.reflect

import wvlet.airspec.AirSpec

case class Person(id:Int, name:String)

object TastySurfaceFactoryTest extends AirSpec {

  test("..") {

    val s = TastySurfaceFactory.ofClass(classOf[Int])
    info(s)

    info(ReflectSurfaceFactory.ofClass(classOf[Person]))

  }

  test("of[A]") {
    val s = TastySurfaceFactory.of[Person]
    info(s.params.mkString(", "))
  }
}
