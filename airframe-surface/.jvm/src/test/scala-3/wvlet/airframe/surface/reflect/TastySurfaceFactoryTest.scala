package wvlet.airframe.surface.reflect

import wvlet.airspec.AirSpec

class Person(id:Int, name:String)

object TastySurfaceFactoryTest extends AirSpec {

  test("..") {

    TastySurfaceFactory.ofClass(classOf[Person])

  }
}
