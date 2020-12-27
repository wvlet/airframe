package dotty.test

import wvlet.log.{LogFormatter, LogLevel, LogSupport, Logger}
import wvlet.airframe._
import wvlet.airframe.surface.Surface

object DITest extends LogSupport {

  trait A

  def run: Unit = {
    info("DI test")
    val s= Surface.of[DesignOptions]
    info(s.params)
    val d = newDesign
    .bind[Int].toInstance(1)
    //   .bind[String].toInstance("hello")

    d.build[Int] { i => }

    info(d)
  }
}