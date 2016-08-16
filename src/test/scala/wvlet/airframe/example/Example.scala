package wvlet.airframe.example

import wvlet.airframe._
import wvlet.log.LogSupport
import wvlet.obj.tag.@@

import scala.util.Random

/**
  *
  */
object Example {

  trait WingType
  trait Left
  trait Right
  case class Wing(name:String)

  class Fuel(var remaining: Int) {
    def burn(r:Int) {
      remaining -= r
    }
  }
  case class PlaneType(tankSize:Int)

  trait Engine {
    val engineType: String
    val fuel = bind[Fuel]

    def run(energy:Int)
  }

  trait AirPlane extends LogSupport {
    val leftWing  = bind[Wing @@ Left]
    val rightWing = bind[Wing @@ Right]
    val engine = bind[Engine]

    info(s"Built a new plane left:${leftWing}, right:${rightWing}, fuel:${engine.fuel.remaining}, engine:${engine.engineType}")

    def start {
      engine.run(1)
      showRemainingFuel
      engine.run(10)
      showRemainingFuel
      engine.run(5)
      showRemainingFuel
    }

    def showRemainingFuel = {
      info(s"remaining fuel: ${engine.fuel.remaining}")
    }
  }

  trait GasolineEngine extends Engine with LogSupport {
    val engineType = "Gasoline Engine"

    def run(energy:Int) {
      fuel.burn(energy)
    }
  }

  trait SolarHybridEngine extends Engine with LogSupport {
    val engineType = "Solar Hybrid Engine"
    val solarPanel = bind[SolarPanel]

    def run(energy:Int) {
      val e = solarPanel.getEnergy
      info(s"Get ${e} solar energy")
      fuel.burn(math.max(0, energy - e))
    }
  }

  case class SolarPanel() {
    def getEnergy = {
      Random.nextInt(10)
    }
  }

  val coreDesign =
    Airframe.newDesign
    .bind[Wing @@ Left].toInstance(new Wing("left"))
    .bind[Wing @@ Right].toInstance(new Wing("right"))
    .bind[PlaneType].toInstance(new PlaneType(50))
    .bind[Fuel].toProvider{ p:PlaneType => new Fuel(p.tankSize * 100) }

  val simplePlaneDesign =
    coreDesign
    .bind[Engine].to[GasolineEngine]

  val hybridPlaneDesign =
    coreDesign
    .bind[PlaneType].toInstance(new PlaneType(10)) // Use a smaller tank
    .bind[Engine].to[SolarHybridEngine]
}

import Example._

class Example extends AirframeSpec {
  "AirframeExample" should {
    "build new planes" in {
      val simplePlane = simplePlaneDesign.build[AirPlane]
      simplePlane.start

      val hybridPlane = hybridPlaneDesign.build[AirPlane]
      hybridPlane.start
    }
  }

}