package wvlet.airframe

import wvlet.airframe.AirframeException.MISSING_SESSION
import wvlet.airframe.lifecycle.LifeCycleManager
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.util.Try
import scala.quoted._

private[airframe] trait SessionImpl { self: Session =>

  /**
    * Build an instance of A. In general this method is necessary only when creating an entry
    * point of your application. When feasible avoid using this method so that Airframe can
    * inject objects where bind[X] is used.
    *
    * @tparam A
    * @return object
    */
  inline def build[A]: A = ${ SessionMacros.buildImpl[A]('self) }

 /**
   * Register an instance to the session to control the life cycle of the object under this session.
   */
  //abstract def register[A](instance: A): Unit = ???
}

private[airframe] object SessionMacros {
  def buildImpl[A](s: Expr[Session])(using Quotes, Type[A]): Expr[A] = {
    '{
      ${newInstanceBinderOf[A]}.apply($s)
    }
  }


  private def newInstanceBinderOf[A](using quotes:Quotes, t: Type[A]): Expr[Session => A] = {
    import AirframeDIMacros.shouldGenerateTraitOf

    if(shouldGenerateTraitOf[A]) {
      import quotes.reflect._
      val tr = TypeRepr.of[A](using t)
      val c = tr.classSymbol.get

      '{
        { (s: Session) => s.get[A](Surface.of[A]) }
      }
      // '{
      //   { (s: Session) => 
      //     s.getOrElse(Surface.of[A], 
      //       new '{t} with DISupport { def session = s }).asInstanceOf[A]
      //   }
      // }
    }
    else {
      '{
        { (s: Session) => s.get[A](Surface.of[A]) }
      }
    }
  }

}
