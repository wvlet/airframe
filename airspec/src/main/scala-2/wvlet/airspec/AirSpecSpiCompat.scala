package wvlet.airspec

import wvlet.airframe.{Design, SourceCode}
import wvlet.airframe.surface.Surface
import wvlet.airspec.spi.{AssertionFailure, InterceptException}

import scala.language.experimental.macros
import scala.reflect.ClassTag

//private[airspec] trait AssertCompat {}

/**
  */
private[airspec] trait AirSpecSpiCompat {

  protected def scalaMajorVersion: Int = 2

  /**
    * This will add Scala.js support to the AirSpec.
    *
    * Scala.js does not support runtime reflection, so the user needs to explicitly create Seq[MethodSurface] at
    * compile-time. This method is a helper method to populate methodSurfaces automatically.
    */
  protected def scalaJsSupport: Unit = {
    wvlet.log
      .Logger("wvlet.airspec").warn(
        s"""scalaJsSupport is deprecated. Use test("...") syntax: ${this.getClass.getName}"""
      )
  }
}

class AirSpecTestBuilder(val spec: AirSpecSpi, val name: String, val design: Design) {
  def apply[R](body: => R): Unit = macro AirSpecMacros.test0Impl[R]
  def apply[D1, R](body: D1 => R): Unit = macro AirSpecMacros.test1Impl[D1, R]
  def apply[D1, D2, R](body: (D1, D2) => R): Unit = macro AirSpecMacros.test2Impl[D1, D2, R]
  def apply[D1, D2, D3, R](body: (D1, D2, D3) => R): Unit = macro AirSpecMacros.test3Impl[D1, D2, D3, R]
  def apply[D1, D2, D3, D4, R](body: (D1, D2, D3, D4) => R): Unit = macro AirSpecMacros.test4Impl[D1, D2, D3, D4, R]
  def apply[D1, D2, D3, D4, D5, R](body: (D1, D2, D3, D4, D5) => R): Unit =
    macro AirSpecMacros.test5Impl[D1, D2, D3, D4, D5, R]
}

object AirSpecTestBuilder {
  implicit class Helper(val v: AirSpecTestBuilder) extends AnyVal {
    def addF0[R](r: Surface, body: wvlet.airframe.LazyF0[R]): Unit = {
      v.spec.addLocalTestDef(AirSpecDefF0(v.name, v.design, r, body))
    }
    def addF1[D1, R](d1: Surface, r: Surface, body: D1 => R): Unit = {
      val spec = body match {
        case s: Seq[_] =>
          // Workaround for: https://github.com/wvlet/airframe/issues/1845
          AirSpecDefF0(v.name, v.design, r, wvlet.airframe.LazyF0(() => body))
        case _ =>
          AirSpecDefF1(v.name, v.design, d1, r, body)
      }
      v.spec.addLocalTestDef(spec)
    }
    def addF2[D1, D2, R](d1: Surface, d2: Surface, r: Surface, body: (D1, D2) => R): Unit = {
      v.spec.addLocalTestDef(AirSpecDefF2(v.name, v.design, d1, d2, r, body))
    }
    def addF3[D1, D2, D3, R](d1: Surface, d2: Surface, d3: Surface, r: Surface, body: (D1, D2, D3) => R): Unit = {
      v.spec.addLocalTestDef(AirSpecDefF3(v.name, v.design, d1, d2, d3, r, body))
    }
    def addF4[D1, D2, D3, D4, R](
        d1: Surface,
        d2: Surface,
        d3: Surface,
        d4: Surface,
        r: Surface,
        body: (D1, D2, D3, D4) => R
    ): Unit = {
      v.spec.addLocalTestDef(AirSpecDefF4(v.name, v.design, d1, d2, d3, d4, r, body))
    }
    def addF5[D1, D2, D3, D4, D5, R](
        d1: Surface,
        d2: Surface,
        d3: Surface,
        d4: Surface,
        d5: Surface,
        r: Surface,
        body: (D1, D2, D3, D4, D5) => R
    ): Unit = {
      v.spec.addLocalTestDef(AirSpecDefF5(v.name, v.design, d1, d2, d3, d4, d5, r, body))
    }
  }
}
