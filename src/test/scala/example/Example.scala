package example

import wvlet.airframe._
import wvlet.log.LogSupport
import wvlet.obj.tag._

import scala.util.Random

/**
  *
  */
object Example {

  trait WingType
  trait Left
  trait Right
  case class Wing(name:String) {
    override def toString = f"Wing($name:[${hashCode()}%x])"
  }

  trait Metric {
    def report(key:String, value:Int)
  }

  object EmptyMetric extends Metric {
    override def report(key: String, value: Int): Unit = {}
  }

  object MetricLogging extends Metric with LogSupport {
    override def report(key: String, value: Int): Unit = {
      warn(s"${key}:${value}")
    }
  }

  trait Fuel {
    lazy val plainType = bind[PlaneType]
    var remaining: Int = plainType.tankSize * 10
    val metric = bind[Metric]

    def burn(r:Int) {
      metric.report("energy.consumption", r)
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

    info(f"Built a new plane left:${leftWing}, right:${rightWing}, fuel:${engine.fuel.remaining}, engine:${engine.engineType}")

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
    .bind[PlaneType].toInstance(PlaneType(50))
    .bind[Metric].toInstance(EmptyMetric)

  val simplePlaneDesign =
    coreDesign
    .bind[Engine].to[GasolineEngine]

  val hybridPlaneDesign =
    coreDesign
    .bind[PlaneType].toInstance(PlaneType(10)) // Use a smaller tank
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

    "extends a component" in {
      val session =
        hybridPlaneDesign
        .bind[Metric].toInstance(MetricLogging)
        .newSession

      val plane = session.build[AirPlane]
      plane.start
    }
  }

}