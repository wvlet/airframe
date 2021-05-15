package dotty.test

import wvlet.log.{LogFormatter, LogLevel, LogSupport, Logger}
import wvlet.airframe._
import wvlet.airframe.surface.Surface

object DITest extends LogSupport {

  case class A(i: Int, s: String)

  def run: Unit = {
    info("DI test")

    val d = Design.newDesign
      .bind[Int].toInstance(1)
      .bind[String].toInstance("hello")

    d.build[A] { x =>
      info(x)
    }

    info(d)
  }
}
