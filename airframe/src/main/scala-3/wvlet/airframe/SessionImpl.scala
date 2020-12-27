package wvlet.airframe

import wvlet.airframe.AirframeException.MISSING_SESSION
import wvlet.airframe.lifecycle.LifeCycleManager
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.util.Try
import scala.quoted._

trait SessionImpl { self: Session =>

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
  //abstract def register[A](instance: A): Unit = ???
}

object SessionImpl {

  def buildImpl[A](s: Expr[Session])(using Quotes, Type[A]): Expr[A] = {
    '{
      ${newInstanceBinderOf[A]}.apply($s)
    }
  }

  def newInstanceBinderOf[A](using quotes:Quotes, t: Type[A]): Expr[Session => A] = {
    import AirframeDIMacros.shouldGenerateTraitOf

    if(shouldGenerateTraitOf[A]) {
      import quotes.reflect._
      val ds = TypeRepr.of[DISupport]
      val expr = '{ (s: Session) => new LogSupport with DISupport { def session = s } }
      val newExpr: Term = expr.asTerm match {
        case Inlined(tree, lst, Block(List(d @ DefDef(sym, tpdef, valdef, tpt, term)), expr)) => 
          val interfaceType = TypeTree.of[A with DISupport]
          val newDef = DefDef.copy(d)(sym, tpdef, valdef, interfaceType, term)
          Inlined(tree, lst, Block(List(newDef), expr))
        case _ =>
          report.error("Unexpected error")
          '{0}.asTerm
      }
      println(newExpr)
      /*
      New(
        TypeTree[TypeRef(TermRef(ThisType(TypeRef(NoPrefix,module class java)),module lang),Object)]
      ),<init>),List()), Ident(E), Ident(DISupport)),
      */
      // TODO Create a new A with DISupport
      newExpr.asExprOf[Session => A]
      // '{
      //   { (s: Session) => s.getOrElse[A](Surface.of[A], 
      //       new ${tr} with DISupport { def session = s  }.asInsatnceOf[A])
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
