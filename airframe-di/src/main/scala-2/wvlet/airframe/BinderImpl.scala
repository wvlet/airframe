package wvlet.airframe

import wvlet.airframe.AirframeMacros._
import wvlet.log.LogSupport

import scala.language.experimental.macros

/**
  */
private[airframe] trait BinderImpl[A] extends LogSupport { self: Binder[A] =>

  /**
    * Bind a singleton instance of B to A
    *
    * @tparam B
    */
  def to[B <: A]: DesignWithContext[B] = macro binderToSingletonOfImpl[B]

  /**
    * Bind an instance of B to A
    *
    * @tparam B
    * @return
    */
  @deprecated("Use toSingletonOf[X] instead. This method will not be available in Scala 3", "21.5.1")
  def toInstanceOf[B <: A]: DesignWithContext[B] = macro binderToImpl[B]

  def toSingletonOf[B <: A]: DesignWithContext[B] = macro binderToSingletonOfImpl[B]

  def toEagerSingletonOf[B <: A]: DesignWithContext[B] = macro binderToEagerSingletonOfImpl[B]

  @deprecated("Use toProvider[X] instead. This method will not be available in Scala 3", "21.5.1")
  def toInstanceProvider[D1](factory: D1 => A): DesignWithContext[A] = macro bindToProvider1[A, D1]
  @deprecated("Use toProvider[X] instead. This method will not be available in Scala 3", "21.5.1")
  def toInstanceProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] = macro bindToProvider2[A, D1, D2]
  @deprecated("Use toProvider[X] instead. This method will not be available in Scala 3", "21.5.1")
  def toInstanceProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] =
    macro bindToProvider3[A, D1, D2, D3]
  @deprecated("Use toProvider[X] instead. This method will not be available in Scala 3", "21.5.1")
  def toInstanceProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] =
    macro bindToProvider4[A, D1, D2, D3, D4]
  @deprecated("Use toProvider[X] instead. This method will not be available in Scala 3", "21.5.1")
  def toInstanceProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] =
    macro bindToProvider5[A, D1, D2, D3, D4, D5]

  def toProvider[D1](factory: D1 => A): DesignWithContext[A] = macro bindToSingletonProvider1[A, D1]
  def toProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] = macro bindToSingletonProvider2[A, D1, D2]
  def toProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] =
    macro bindToSingletonProvider3[A, D1, D2, D3]
  def toProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] =
    macro bindToSingletonProvider4[A, D1, D2, D3, D4]
  def toProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] =
    macro bindToSingletonProvider5[A, D1, D2, D3, D4, D5]

  @deprecated("Use toProvider[X] instead. This method will not be available in Scala 3", "21.5.1")
  def toSingletonProvider[D1](factory: D1 => A): DesignWithContext[A] = macro bindToSingletonProvider1[A, D1]
  @deprecated("Use toProvider[X] instead. This method will not be available in Scala 3", "21.5.1")
  def toSingletonProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] =
    macro bindToSingletonProvider2[A, D1, D2]
  @deprecated("Use toProvider[X] instead. This method will not be available in Scala 3", "21.5.1")
  def toSingletonProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] =
    macro bindToSingletonProvider3[A, D1, D2, D3]
  @deprecated("Use toProvider[X] instead. This method will not be available in Scala 3", "21.5.1")
  def toSingletonProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] =
    macro bindToSingletonProvider4[A, D1, D2, D3, D4]
  @deprecated("Use toProvider[X] instead. This method will not be available in Scala 3", "21.5.1")
  def toSingletonProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] =
    macro bindToSingletonProvider5[A, D1, D2, D3, D4, D5]

  def toEagerSingletonProvider[D1](factory: D1 => A): DesignWithContext[A] = macro bindToEagerSingletonProvider1[A, D1]
  def toEagerSingletonProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] =
    macro bindToEagerSingletonProvider2[A, D1, D2]
  def toEagerSingletonProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] =
    macro bindToEagerSingletonProvider3[A, D1, D2, D3]
  def toEagerSingletonProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] =
    macro bindToEagerSingletonProvider4[A, D1, D2, D3, D4]
  def toEagerSingletonProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] =
    macro bindToEagerSingletonProvider5[A, D1, D2, D3, D4, D5]
}
