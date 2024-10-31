package wvlet.airframe

import wvlet.log.LogSupport

import scala.language.experimental.macros

/**
  * Immutable airframe design.
  *
  * Design instance does not hold any duplicate bindings for the same Surface.
  */
private[airframe] trait DesignImpl extends LogSupport {
  def bind[A]: Binder[A] = macro AirframeMacros.designBindImpl[A]
  def remove[A]: Design = macro AirframeMacros.designRemoveImpl[A]

  def bindInstance[A](obj: A): DesignWithContext[A] = macro AirframeMacros.designBindInstanceImpl[A]
  def bindSingleton[A]: DesignWithContext[A] = macro AirframeMacros.designBindSingletonImpl[A]
  def bindImpl[A, B <: A]: DesignWithContext[B] = macro AirframeMacros.designBindImplImpl[A, B]
  def bindProvider[D1, A](f: D1 => A): DesignWithContext[A] = macro AirframeMacros.designBindProvider1Impl[D1, A]
  def bindProvider[D1, D2, A](f: (D1, D2) => A): DesignWithContext[A] = macro AirframeMacros.designBindProvider2Impl[D1, D2, A]
  def bindProvider[D1, D2, D3, A](f: (D1, D2, D3) => A): DesignWithContext[A] =
    macro AirframeMacros.designBindProvider3Impl[D1, D2, D3, A]
  def bindProvider[D1, D2, D3, D4, A](f: (D1, D2, D3, D4) => A): DesignWithContext[A] =
    macro AirframeMacros.designBindProvider4Impl[D1, D2, D3, D4, A]
  def bindProvider[D1, D2, D3, D4, D5, A](f: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] =
    macro AirframeMacros.designBindProvider5Impl[D1, D2, D3, D4, D5, A]

  /**
    * A helper method of creating a new session and an instance of A. This method is useful when you only need to use A
    * as an entry point of your program. After executing the body, the sesion will be closed.
    *
    * @param body
    * @tparam A
    * @return
    */
  def build[A](body: A => Any): Any = macro AirframeMacros.buildWithSession[A]

  /**
    * Execute a given code block by building A using this design, and return B
    */
  def run[A, B](body: A => B): B = macro AirframeMacros.runWithSession[A, B]
}
