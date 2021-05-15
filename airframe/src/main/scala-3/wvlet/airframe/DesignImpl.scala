package wvlet.airframe

import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

/**
  * Immutable airframe design.
  *
  * Design instance does not hold any duplicate bindings for the same Surface.
  */
private[airframe] trait DesignImpl extends LogSupport { self: Design =>
  inline def bind[A]: Binder[A] = ${ DesignMacros.designBind[A]('self) }

  inline def remove[A]: Design = {
    {
      val target = Surface.of[A]
      new Design(self.designOptions, self.binding.filterNot(_.from == target), self.hooks)
    }
  }

  /**
    * A helper method of creating a new session and an instance of A.
    * This method is useful when you only need to use A as an entry point of your program.
    * After executing the body, the sesion will be closed.
    *
    * @param body
    * @tparam A
    * @return
    */
  inline def build[A](body: A => Any): Any = {
    {
      self.withSession { session =>
        val a = session.build[A]
        body(a)
      }
    }
  }

  /**
    * Execute a given code block by building A using this design, and return B
    */
  inline def run[A, B](body: A => B): B = {
    {
      self.withSession { session =>
        val a = session.build[A]
        body(a)
      }
    }.asInstanceOf[B]
  }

}

private[airframe] object DesignMacros {
  import scala.quoted._

  def designBind[A](design:Expr[Design])(using quotes:Quotes, tpe: Type[A]): Expr[Binder[A]] = {
    '{
      new Binder(${design}, Surface.of[A], SourceCode()).asInstanceOf[Binder[A]]
    }
  }
}
