package wvlet.airframe

import wvlet.airframe.surface.Surface

private[airframe] trait SessionImpl { self: Session =>

  /**
    * Build an instance of A. In general this method is necessary only when creating an entry
    * point of your application. When feasible avoid using this method so that Airframe can
    * inject objects where bind[X] is used.
    *
    * @tparam A
    * @return object
    */
  inline def build[A]: A = ${ SessionImpl.buildImpl[A]('self) }

  /**
    * Register an instance to the session to control the life cycle of the object under this session.
    */
  def register[A](instance: A): Unit
}


private[airframe] object SessionImpl {
  import scala.quoted._

  def buildImpl[A](session: Expr[Session])(using Quotes, Type[A]): Expr[A] = {
    '{ ${session}.get[A](Surface.of[A]) }
  }
}
