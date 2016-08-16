package wvlet.airframe.example

import wvlet.airframe._
import wvlet.log.LogSupport
import wvlet.obj.tag.@@

/**
  *
  */
object Example {

  trait WingType
  trait Left
  trait Right
  case class Wing(name:String)

  class Fuel(var remaining: Int)
  case class PlaneType(size:Int)

  trait AirPlane extends LogSupport {
    val leftWing  = bind[Wing @@ Left]
    val rightWing = bind[Wing @@ Right]
    val fuel = bind{ p:PlaneType => new Fuel(p.size * 100) }

    info(s"Built a new plane left:${leftWing}, right:${rightWing}, fuel:${fuel.remaining}")
  }

  val coreDesign =
    Airframe.newDesign
    .bind[Wing @@ Left].toInstance(new Wing("left"))
    .bind[Wing @@ Right].toInstance(new Wing("right"))

  val smallPlaneDesign =
    coreDesign
    .bind[PlaneType].toInstance(new PlaneType(10))

  val largePlaneDesign =
    coreDesign
    .bind[PlaneType].toInstance(new PlaneType(100))


}

import Example._

class Example extends AirframeSpec {

  "AirframeExample" should {
    "build new planes" in {
      val smalLPlane = smallPlaneDesign.build[AirPlane]
      val largePlane = largePlaneDesign.build[AirPlane]
    }
  }

}