package wvlet.airframe

import wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY
import wvlet.airframe.lifecycle.{AFTER_START, BEFORE_SHUTDOWN, ON_INIT, ON_INJECT, ON_SHUTDOWN, ON_START}
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport
import wvlet.airframe.Binder._

/**
  */
private[airframe] trait BinderImpl[A] extends LogSupport { self: Binder[A] =>

  /**
    * Bind a singleton instance of B to A
    *
    * @tparam B
    */
  def to[B <: A]: DesignWithContext[B] = ???

  /**
    * Bind an instance of B to A
    *
    * @tparam B
    * @return
    */
  def toInstanceOf[B <: A]: DesignWithContext[B] = ???
  def toSingletonOf[B <: A]: DesignWithContext[B] = ???
  def toEagerSingletonOf[B <: A]: DesignWithContext[B] = ???

  def toInstanceProvider[D1](factory: D1 => A): DesignWithContext[A] = ???
  def toInstanceProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] = ???
  def toInstanceProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] = ???
  def toInstanceProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] = ???
  def toInstanceProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] = ???

  def toProvider[D1](factory: D1 => A): DesignWithContext[A] = ???
  def toProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] = ???
  def toProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] = ???
  def toProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] = ???
  def toProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] = ???

  def toSingletonProvider[D1](factory: D1 => A): DesignWithContext[A] = ???
  def toSingletonProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] = ???
  def toSingletonProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] = ???
  def toSingletonProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] = ???
  def toSingletonProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] = ???

  def toEagerSingletonProvider[D1](factory: D1 => A): DesignWithContext[A] = ???
  def toEagerSingletonProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] = ???
  def toEagerSingletonProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] = ???
  def toEagerSingletonProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] = ???
  def toEagerSingletonProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] = ???
}
