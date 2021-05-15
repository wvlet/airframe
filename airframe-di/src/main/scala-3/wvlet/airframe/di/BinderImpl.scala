package wvlet.airframe.di

import wvlet.log.LogSupport
/**
  */
private[di] trait BinderImpl[A] extends LogSupport { self: Binder[A] =>

  /**
    * Bind a singleton instance of B to A
    *
    * @tparam B
    */
  def to[B <: A]: DesignWithContext[B] = ???
  def toEagerSingletonOf[B <: A]: DesignWithContext[B] = ???

  def toProvider[D1](factory: D1 => A): DesignWithContext[A] = ???
  def toProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] = ???
  def toProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] = ???
  def toProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] = ???
  def toProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] = ???

  def toEagerSingletonProvider[D1](factory: D1 => A): DesignWithContext[A] = ???
  def toEagerSingletonProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] = ???
  def toEagerSingletonProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] = ???
  def toEagerSingletonProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] = ???
  def toEagerSingletonProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] = ???
}
