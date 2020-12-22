package wvlet.airframe

import wvlet.airframe.AirframeException.MISSING_SESSION
import wvlet.airframe.lifecycle.LifeCycleManager
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.language.experimental.macros
import scala.util.Try
import scala.reflect.runtime.{universe => ru}

private[airframe] trait SessionImpl { self: Session =>

  /**
    * Build an instance of A. In general this method is necessary only when creating an entry
    * point of your application. When feasible avoid using this method so that Airframe can
    * inject objects where bind[X] is used.
    *
    * @tparam A
    * @return object
    */
  def build[A]: A = macro AirframeMacros.buildImpl[A]

  /**
    * Register an instance to the session to control the life cycle of the object under this session.
    */
  def register[A: ru.TypeTag](instance: A): Unit
}
