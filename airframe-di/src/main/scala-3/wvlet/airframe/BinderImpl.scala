package wvlet.airframe

import wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport
import wvlet.airframe.Binder.*

/**
  */
private[airframe] trait BinderImpl[A] extends LogSupport { self: Binder[A] =>

  /**
    * Bind a singleton instance of B to A
    *
    * @tparam B
    */
  inline def to[B <: A]: DesignWithContext[B] = {
    {
      // registerTraitFactory[B]
      val to = Surface.of[B]
      if self.from == to then {
        wvlet.log.Logger("wvlet.airframe.Binder").warn("Binding to the same type is not allowed: " + to.toString)
        throw new wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY(List(to), SourceCode())
      }
      self.design.addBinding[B](SingletonBinding(self.from, to, false, self.sourceCode))
    }
  }

  inline def toEagerSingletonOf[B <: A]: DesignWithContext[B] = {
    {
      // registerTraitFactory[B]
      val to = Surface.of[B]
      if self.from == to then {
        wvlet.log.Logger("wvlet.airframe.Binder").warn("Binding to the same type is not allowed: " + to.toString)
        throw new wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY(List(to), SourceCode())
      }
      self.design.addBinding[B](SingletonBinding(self.from, to, true, self.sourceCode))
    }
  }

  inline def toProvider[D1](factory: D1 => A): DesignWithContext[A] = {
    // registerTraitFactory[D1]
    self.design.addBinding[A](
      ProviderBinding(
        DependencyFactory(self.from, Seq(Surface.of[D1]), factory),
        true,
        false,
        SourceCode()
      )
    )
  }
  inline def toProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] = {
    // registerTraitFactory[D1]
    // registerTraitFactory[D2]
    self.design.addBinding[A](
      ProviderBinding(
        DependencyFactory(self.from, Seq(Surface.of[D1], Surface.of[D2]), factory),
        true,
        false,
        SourceCode()
      )
    )
  }
  inline def toProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] = {
    // registerTraitFactory[D1]
    // registerTraitFactory[D2]
    // registerTraitFactory[D3]
    self.design.addBinding[A](
      ProviderBinding(
        DependencyFactory(self.from, Seq(Surface.of[D1], Surface.of[D2], Surface.of[D3]), factory),
        true,
        false,
        SourceCode()
      )
    )
  }
  inline def toProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] = {
    // registerTraitFactory[D1]
    // registerTraitFactory[D2]
    // registerTraitFactory[D3]
    // registerTraitFactory[D4]
    self.design.addBinding[A](
      ProviderBinding(
        DependencyFactory(self.from, Seq(Surface.of[D1], Surface.of[D2], Surface.of[D3], Surface.of[D4]), factory),
        true,
        false,
        SourceCode()
      )
    )
  }
  inline def toProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] = {
    // registerTraitFactory[D1]
    // registerTraitFactory[D2]
    // registerTraitFactory[D3]
    // registerTraitFactory[D4]
    // registerTraitFactory[D5]
    self.design.addBinding[A](
      ProviderBinding(
        DependencyFactory(
          self.from,
          Seq(Surface.of[D1], Surface.of[D2], Surface.of[D3], Surface.of[D4], Surface.of[D5]),
          factory
        ),
        true,
        false,
        SourceCode()
      )
    )
  }

  inline def toEagerSingletonProvider[D1](factory: D1 => A): DesignWithContext[A] = {
    // registerTraitFactory[D1]
    self.design.addBinding[A](
      ProviderBinding(
        DependencyFactory(self.from, Seq(Surface.of[D1]), factory),
        true,
        true,
        SourceCode()
      )
    )
  }
  inline def toEagerSingletonProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] = {
    // registerTraitFactory[D1]
    // registerTraitFactory[D2]
    self.design.addBinding[A](
      ProviderBinding(
        DependencyFactory(self.from, Seq(Surface.of[D1], Surface.of[D2]), factory),
        true,
        true,
        SourceCode()
      )
    )
  }
  inline def toEagerSingletonProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] = {
    // registerTraitFactory[D1]
    // registerTraitFactory[D2]
    // registerTraitFactory[D3]
    self.design.addBinding[A](
      ProviderBinding(
        DependencyFactory(self.from, Seq(Surface.of[D1], Surface.of[D2], Surface.of[D3]), factory),
        true,
        true,
        SourceCode()
      )
    )
  }
  inline def toEagerSingletonProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] = {
    // registerTraitFactory[D1]
    // registerTraitFactory[D2]
    // registerTraitFactory[D3]
    // registerTraitFactory[D4]
    self.design.addBinding[A](
      ProviderBinding(
        DependencyFactory(self.from, Seq(Surface.of[D1], Surface.of[D2], Surface.of[D3], Surface.of[D4]), factory),
        true,
        true,
        SourceCode()
      )
    )
  }
  inline def toEagerSingletonProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] = {
    // registerTraitFactory[D1]
    // registerTraitFactory[D2]
    // registerTraitFactory[D3]
    // registerTraitFactory[D4]
    // registerTraitFactory[D5]
    self.design.addBinding[A](
      ProviderBinding(
        DependencyFactory(
          self.from,
          Seq(Surface.of[D1], Surface.of[D2], Surface.of[D3], Surface.of[D4], Surface.of[D5]),
          factory
        ),
        true,
        true,
        SourceCode()
      )
    )
  }
}
