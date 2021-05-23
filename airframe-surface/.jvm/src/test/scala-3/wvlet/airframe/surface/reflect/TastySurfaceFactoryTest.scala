package wvlet.airframe.surface.reflect

import wvlet.airspec.AirSpec


case class Person(id:Int, name:String) {
  def hello: String = "hello"
}



object TastySurfaceFactoryTest extends AirSpec {

  test("of[A]") {
    val s = TastySurfaceFactory.of[Person]
    info(s.params.mkString(", "))
  }

  test("ofClass") {
    val s = ReflectSurfaceFactory.ofClass(classOf[Person])
    info(s)

    val s2 = ReflectSurfaceFactory.ofClass(classOf[Person])
    info(s2)
  }

  test("methodsOf") {
    pending("runtime error is shown")
    val m = TastySurfaceFactory.methodsOfClass(classOf[Person])
    info(m.mkString(", "))
  }
}
