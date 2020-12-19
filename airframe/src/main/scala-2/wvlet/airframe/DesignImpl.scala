package wvlet.airframe

import wvlet.airframe.Binder.Binding
import wvlet.airframe.surface.Surface
import wvlet.airframe.tracing.{DIStats, Tracer}
import wvlet.log.LogSupport

import scala.language.experimental.macros
import Design._
import DesignOptions._
import wvlet.airframe.lifecycle.LifeCycleHookType

/**
  * Immutable airframe design.
  *
  * Design instance does not hold any duplicate bindings for the same Surface.
  */
private[airframe] trait DesignImpl extends LogSupport {
  def bind[A]: Binder[A] = macro AirframeMacros.designBindImpl[A]
  def remove[A]: Design = macro AirframeMacros.designRemoveImpl[A]

  /**
    * A helper method of creating a new session and an instance of A.
    * This method is useful when you only need to use A as an entry point of your program.
    * After executing the body, the sesion will be closed.
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
