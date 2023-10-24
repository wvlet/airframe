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

class AirSpecTestBuilder(val spec: AirSpecSpi, val name: String, val design: Design => Design)
    extends wvlet.log.LogSupport {
  inline def apply[R](inline body: => R): Unit                  = ${ AirSpecMacros.test0Impl[R]('this, 'body) }
  inline def apply[D1, R](inline body: D1 => R): Unit           = ${ AirSpecMacros.test1Impl[D1, R]('this, 'body) }
  inline def apply[D1, D2, R](inline body: (D1, D2) => R): Unit = ${ AirSpecMacros.test2Impl[D1, D2, R]('this, 'body) }
  inline def apply[D1, D2, D3, R](inline body: (D1, D2, D3) => R): Unit = ${
    AirSpecMacros.test3Impl[D1, D2, D3, R]('this, 'body)
  }
  inline def apply[D1, D2, D3, D4, R](inline body: (D1, D2, D3, D4) => R): Unit = ${
    AirSpecMacros.test4Impl[D1, D2, D3, D4, R]('this, 'body)
  }
  inline def apply[D1, D2, D3, D4, D5, R](inline body: (D1, D2, D3, D4, D5) => R): Unit = ${
    AirSpecMacros.test5Impl[D1, D2, D3, D4, D5, R]('this, 'body)
  }
}

object AirSpecTestBuilder extends wvlet.log.LogSupport {
  implicit class Helper(val v: AirSpecTestBuilder) extends AnyVal {

    def addF0[R](body: wvlet.airframe.LazyF0[R]): Unit = {
      v.spec.addLocalTestDef(AirSpecDefF0(v.name, v.design, body))
    }
    def addF1[D1, R](d1: Surface, body: D1 => R): Unit = {
      v.spec.addLocalTestDef(AirSpecDefF1(v.name, v.design, d1, body))
    }
    def addF2[D1, D2, R](d1: Surface, d2: Surface, body: (D1, D2) => R): Unit = {
      v.spec.addLocalTestDef(AirSpecDefF2(v.name, v.design, d1, d2, body))
    }
    def addF3[D1, D2, D3, R](d1: Surface, d2: Surface, d3: Surface, body: (D1, D2, D3) => R): Unit = {
      v.spec.addLocalTestDef(AirSpecDefF3(v.name, v.design, d1, d2, d3, body))
    }
    def addF4[D1, D2, D3, D4, R](
        d1: Surface,
        d2: Surface,
        d3: Surface,
        d4: Surface,
        body: (D1, D2, D3, D4) => R
    ): Unit = {
      v.spec.addLocalTestDef(AirSpecDefF4(v.name, v.design, d1, d2, d3, d4, body))
    }
    def addF5[D1, D2, D3, D4, D5, R](
        d1: Surface,
        d2: Surface,
        d3: Surface,
        d4: Surface,
        d5: Surface,
        body: (D1, D2, D3, D4, D5) => R
    ): Unit = {
      v.spec.addLocalTestDef(AirSpecDefF5(v.name, v.design, d1, d2, d3, d4, d5, body))
    }
  }
}

private[airspec] object AirSpecMacros {
  import scala.quoted.*

  def test0Impl[R](self: Expr[AirSpecTestBuilder], body: Expr[R])(using Type[R], Quotes): Expr[Unit] = {
    '{
      import AirSpecTestBuilder.*
      ${ self }.addF0(LazyF0(${ body }))
    }
  }

  def test1Impl[D1, R](self: Expr[AirSpecTestBuilder], body: Expr[D1 => R])(using
      Type[D1],
      Type[R],
      Quotes
  ): Expr[Unit] = {
    import quotes.reflect.*
    body match {
      // Workaround for https://github.com/wvlet/airframe/issues/1845. If the return type of a test code block is
      // Nil or Seq[_], Scala 3 complier passes Function1[Int, R] as a body even if the code block take no argument.
      case '{ $x: t } if TypeRepr.of[t] =:= TypeRepr.of[Nil.type] || TypeRepr.of[t] <:< TypeRepr.of[Seq[_]] =>
        '{
          import AirSpecTestBuilder.*
          ${ self }.addF0(LazyF0.apply(${ body }))
        }
      case _ =>
        '{
          import AirSpecTestBuilder.*
          ${ self }.addF1(Surface.of[D1], ${ body })
        }
    }
  }

  def test2Impl[D1, D2, R](
      self: Expr[AirSpecTestBuilder],
      body: Expr[(D1, D2) => R]
  )(using Type[D1], Type[D2], Type[R], Quotes): Expr[Unit] = {
    '{
      import AirSpecTestBuilder.*
      ${ self }.addF2(Surface.of[D1], Surface.of[D2], ${ body })
    }
  }

  def test3Impl[D1, D2, D3, R](
      self: Expr[AirSpecTestBuilder],
      body: Expr[(D1, D2, D3) => R]
  )(using Type[D1], Type[D2], Type[D3], Type[R], Quotes): Expr[Unit] = {
    '{
      import AirSpecTestBuilder.*
      ${ self }.addF3(Surface.of[D1], Surface.of[D2], Surface.of[D3], ${ body })
    }
  }

  def test4Impl[D1, D2, D3, D4, R](
      self: Expr[AirSpecTestBuilder],
      body: Expr[(D1, D2, D3, D4) => R]
  )(using Type[D1], Type[D2], Type[D3], Type[D4], Type[R], Quotes): Expr[Unit] = {
    '{
      import AirSpecTestBuilder.*
      ${ self }.addF4(Surface.of[D1], Surface.of[D2], Surface.of[D3], Surface.of[D4], ${ body })
    }
  }

  def test5Impl[D1, D2, D3, D4, D5, R](
      self: Expr[AirSpecTestBuilder],
      body: Expr[(D1, D2, D3, D4, D5) => R]
  )(using Type[D1], Type[D2], Type[D3], Type[D4], Type[D5], Type[R], Quotes): Expr[Unit] = {
    '{
      import AirSpecTestBuilder.*
      ${ self }.addF5(
        Surface.of[D1],
        Surface.of[D2],
        Surface.of[D3],
        Surface.of[D4],
        Surface.of[D5],
        ${ body }
      )
    }
  }
}
