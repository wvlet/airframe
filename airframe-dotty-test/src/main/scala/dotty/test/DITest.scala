package dotty.test

import wvlet.log.{LogFormatter, LogLevel, LogSupport, Logger}
import wvlet.airframe._
import wvlet.airframe.surface.Surface

object DITest extends LogSupport {

  def run: Unit = {
    info("DI test")
    val s= Surface.of[DesignOptions]
    info(s.params)
    val d = newDesign
    .bind[Int].toInstance(1)
    //   .bind[String].toInstance("hello")


    info(d)
  }
}