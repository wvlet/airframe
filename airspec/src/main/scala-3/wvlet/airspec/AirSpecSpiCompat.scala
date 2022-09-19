package wvlet.airspec

import wvlet.airframe.{Design, LazyF0, SourceCode}
import wvlet.airframe.surface.Surface
import wvlet.airspec.spi.{AssertionFailure, InterceptException}

import scala.reflect.ClassTag

/**
  */
private[airspec] trait AirSpecSpiCompat { self: AirSpecSpi =>
  protected def scalaMajorVersion: Int = 3

  protected def scalaJsSupport: Unit = {
    wvlet.log
      .Logger("wvlet.airspec").warn(
        s"""scalaJsSupport is deprecated. Use test("...") syntax: ${this.getClass.getName}"""
      )
  }
}

class AirSpecTestBuilder(val spec: AirSpecSpi, val name: String, val design: Design) extends wvlet.log.LogSupport {
  inline def apply[R](body: => R): Unit                  = ${ AirSpecMacros.test0Impl[R]('this, 'body) }
  inline def apply[D1, R](body: D1 => R): Unit           = ${ AirSpecMacros.test1Impl[R, D1]('this, 'body) }
  inline def apply[D1, D2, R](body: (D1, D2) => R): Unit = ${ AirSpecMacros.test2Impl[R, D1, D2]('this, 'body) }
  inline def apply[D1, D2, D3, R](body: (D1, D2, D3) => R): Unit = ${
    AirSpecMacros.test3Impl[R, D1, D2, D3]('this, 'body)
  }
  inline def apply[D1, D2, D3, D4, R](body: (D1, D2, D3, D4) => R): Unit = ${
    AirSpecMacros.test4Impl[R, D1, D2, D3, D4]('this, 'body)
  }
  inline def apply[D1, D2, D3, D4, D5, R](body: (D1, D2, D3, D4, D5) => R): Unit = ${
    AirSpecMacros.test5Impl[R, D1, D2, D3, D4, D5]('this, 'body)
  }
}

object AirSpecTestBuilder extends wvlet.log.LogSupport {
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

private[airspec] object AirSpecMacros {
  import scala.quoted._

  def test0Impl[R](self: Expr[AirSpecTestBuilder], body: Expr[_])(using Type[R], Quotes): Expr[Unit] = {
    '{
      import AirSpecTestBuilder._
      ${ self }.addF0(Surface.of[R], LazyF0(${ body }))
    }
  }

  def test1Impl[R, D1](self: Expr[AirSpecTestBuilder], body: Expr[D1 => R])(using
      Type[R],
      Type[D1],
      Quotes
  ): Expr[Unit] = {
    '{
      import AirSpecTestBuilder._
      ${ self }.addF1(Surface.of[D1], Surface.of[R], ${ body })
    }
  }

  def test2Impl[R, D1, D2](
      self: Expr[AirSpecTestBuilder],
      body: Expr[(D1, D2) => R]
  )(using Type[R], Type[D1], Type[D2], Quotes): Expr[Unit] = {
    '{
      import AirSpecTestBuilder._
      ${ self }.addF2(Surface.of[D1], Surface.of[D2], Surface.of[R], ${ body })
    }
  }

  def test3Impl[R, D1, D2, D3](
      self: Expr[AirSpecTestBuilder],
      body: Expr[(D1, D2, D3) => R]
  )(using Type[R], Type[D1], Type[D2], Type[D3], Quotes): Expr[Unit] = {
    '{
      import AirSpecTestBuilder._
      ${ self }.addF3(Surface.of[D1], Surface.of[D2], Surface.of[D3], Surface.of[R], ${ body })
    }
  }

  def test4Impl[R, D1, D2, D3, D4](
      self: Expr[AirSpecTestBuilder],
      body: Expr[(D1, D2, D3, D4) => R]
  )(using Type[R], Type[D1], Type[D2], Type[D3], Type[D4], Quotes): Expr[Unit] = {
    '{
      import AirSpecTestBuilder._
      ${ self }.addF4(Surface.of[D1], Surface.of[D2], Surface.of[D3], Surface.of[D4], Surface.of[R], ${ body })
    }
  }

  def test5Impl[R, D1, D2, D3, D4, D5](
      self: Expr[AirSpecTestBuilder],
      body: Expr[(D1, D2, D3, D4, D5) => R]
  )(using Type[R], Type[D1], Type[D2], Type[D3], Type[D4], Type[D5], Quotes): Expr[Unit] = {
    '{
      import AirSpecTestBuilder._
      ${ self }.addF5(
        Surface.of[D1],
        Surface.of[D2],
        Surface.of[D3],
        Surface.of[D4],
        Surface.of[D5],
        Surface.of[R],
        ${ body }
      )
    }
  }
}
