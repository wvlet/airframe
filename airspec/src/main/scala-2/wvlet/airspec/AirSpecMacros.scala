package wvlet.airspec

import scala.language.experimental.macros
import scala.reflect.macros.{blackbox => sm}

/**
  */
private[airspec] object AirSpecMacros {
  def sourceCode(c: sm.Context): c.Tree = {
    import c.universe._
    c.internal.enclosingOwner
    val pos = c.enclosingPosition
    q"wvlet.airframe.SourceCode(${""}, ${pos.source.file.name}, ${pos.line}, ${pos.column})"
  }

  def pendingImpl(c: sm.Context): c.Tree = {
    import c.universe._
    q"""
       throw wvlet.airspec.spi.Pending("pending", ${sourceCode(c)})
     """
  }

  def test0Impl[R: c.WeakTypeTag](c: sm.Context)(body: c.Tree): c.Tree = {
    import c.universe._
    val r = implicitly[c.WeakTypeTag[R]].tpe
    q"""{
      import wvlet.airspec.AirSpecTestBuilder._
      val self = ${c.prefix}
      val surface = wvlet.airframe.surface.Surface.of[${r}]
      self.addF0(surface, wvlet.airframe.LazyF0(${body}))
    }"""
  }

  def test1Impl[D1: c.WeakTypeTag, R: c.WeakTypeTag](c: sm.Context)(body: c.Tree): c.Tree = {
    import c.universe._
    val d1 = implicitly[c.WeakTypeTag[D1]].tpe
    val r  = implicitly[c.WeakTypeTag[R]].tpe
    q"""{
      import wvlet.airspec.AirSpecTestBuilder._
      val self = ${c.prefix}
      val surface = wvlet.airframe.surface.Surface.of[${r}]
      val d1 = wvlet.airframe.surface.Surface.of[${d1}]
      self.addF1(d1, surface, ${body})
    }"""
  }

  def test2Impl[D1: c.WeakTypeTag, D2: c.WeakTypeTag, R: c.WeakTypeTag](c: sm.Context)(body: c.Tree): c.Tree = {
    import c.universe._
    val d1 = implicitly[c.WeakTypeTag[D1]].tpe
    val d2 = implicitly[c.WeakTypeTag[D2]].tpe
    val r  = implicitly[c.WeakTypeTag[R]].tpe
    q"""{
      import wvlet.airspec.AirSpecTestBuilder._
      val self = ${c.prefix}
      val surface = wvlet.airframe.surface.Surface.of[${r}]
      val d1 = wvlet.airframe.surface.Surface.of[${d1}]
      val d2 = wvlet.airframe.surface.Surface.of[${d2}]
      self.addF2(d1, d2, surface, ${body})
    }"""
  }

  def test3Impl[D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, R: c.WeakTypeTag](
      c: sm.Context
  )(body: c.Tree): c.Tree = {
    import c.universe._
    val d1 = implicitly[c.WeakTypeTag[D1]].tpe
    val d2 = implicitly[c.WeakTypeTag[D2]].tpe
    val d3 = implicitly[c.WeakTypeTag[D3]].tpe
    val r  = implicitly[c.WeakTypeTag[R]].tpe
    q"""{
      import wvlet.airspec.AirSpecTestBuilder._
      val self = ${c.prefix}
      val surface = wvlet.airframe.surface.Surface.of[${r}]
      val d1 = wvlet.airframe.surface.Surface.of[${d1}]
      val d2 = wvlet.airframe.surface.Surface.of[${d2}]
      val d3 = wvlet.airframe.surface.Surface.of[${d3}]
      self.addF3(d1, d2, d3, surface, ${body})
    }"""
  }

  def test4Impl[D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag, R: c.WeakTypeTag](
      c: sm.Context
  )(body: c.Tree): c.Tree = {
    import c.universe._
    val d1 = implicitly[c.WeakTypeTag[D1]].tpe
    val d2 = implicitly[c.WeakTypeTag[D2]].tpe
    val d3 = implicitly[c.WeakTypeTag[D3]].tpe
    val d4 = implicitly[c.WeakTypeTag[D4]].tpe
    val r  = implicitly[c.WeakTypeTag[R]].tpe
    q"""{
      import wvlet.airspec.AirSpecTestBuilder._
      val self = ${c.prefix}
      val surface = wvlet.airframe.surface.Surface.of[${r}]
      val d1 = wvlet.airframe.surface.Surface.of[${d1}]
      val d2 = wvlet.airframe.surface.Surface.of[${d2}]
      val d3 = wvlet.airframe.surface.Surface.of[${d3}]
      val d4 = wvlet.airframe.surface.Surface.of[${d4}]
      self.addF4(d1, d2, d3, d4, surface, ${body})
    }"""
  }

  def test5Impl[
      D1: c.WeakTypeTag,
      D2: c.WeakTypeTag,
      D3: c.WeakTypeTag,
      D4: c.WeakTypeTag,
      D5: c.WeakTypeTag,
      R: c.WeakTypeTag
  ](
      c: sm.Context
  )(body: c.Tree): c.Tree = {
    import c.universe._
    val d1 = implicitly[c.WeakTypeTag[D1]].tpe
    val d2 = implicitly[c.WeakTypeTag[D2]].tpe
    val d3 = implicitly[c.WeakTypeTag[D3]].tpe
    val d4 = implicitly[c.WeakTypeTag[D4]].tpe
    val d5 = implicitly[c.WeakTypeTag[D5]].tpe
    val r  = implicitly[c.WeakTypeTag[R]].tpe
    q"""{
      import wvlet.airspec.AirSpecTestBuilder._
      val self = ${c.prefix}
      val surface = wvlet.airframe.surface.Surface.of[${r}]
      val d1 = wvlet.airframe.surface.Surface.of[${d1}]
      val d2 = wvlet.airframe.surface.Surface.of[${d2}]
      val d3 = wvlet.airframe.surface.Surface.of[${d3}]
      val d4 = wvlet.airframe.surface.Surface.of[${d4}]
      val d5 = wvlet.airframe.surface.Surface.of[${d5}]
      self.addF5(d1, d2, d3, d4, d5, surface, ${body})
    }"""
  }

}
