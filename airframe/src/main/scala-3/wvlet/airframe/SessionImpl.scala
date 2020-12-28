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

      class MyTraverser extends TreeTraverser {
        override def traverseTree(tree: Tree)(owner: Symbol): Unit = {}
      }

      val buff = new StringBuilder
      val traverser = new TreeTraverser {
        override def traverseTree(tree: Tree)(owner: Symbol): Unit = tree match {
          case tree: TypeBoundsTree =>
            buff.append(tree.tpe.show(using Printer.TypeReprStructure))
            buff.append("\n\n")
            traverseTreeChildren(tree)(owner)
          case tree: TypeTree =>
            buff.append(tree.tpe.show(using Printer.TypeReprStructure))
            buff.append("\n\n")
            traverseTreeChildren(tree)(owner)
          case _ =>
            super.traverseTree(tree)(owner)
        }
      }


      val ds = TypeRepr.of[DISupport]

      // Create this expression:
      // '{
      //   { (s: Session) => s.getOrElse[A](Surface.of[A], new A with DISupport { def session = s }.asInsatnceOf[A]) }
      // }
      val expr = '{ (s: Session) => new LogSupport with DISupport { def session = s }.asInstanceOf[LogSupport] }
      //println(expr.asTerm)
      val newExpr: Term = expr.asTerm match {
        case Inlined(tree, lst, Block(List(d @ DefDef(sym, tpdef, valdef, tpt, Some(term))), expr)) => 
          val interfaceType = TypeTree.of[A with DISupport]
          
          traverser.traverseTree(term)(Symbol.spliceOwner)
          println(buff.result)

          val newBlock: Term = term match {
            case Block(l, TypeApply(fun @ Select(Block(List(b1), b2 @ Typed(anonTerm, anonTT)), str), _)) =>
              printTree(b1)
              b1 match {
                case td : TypeDef =>
                  println("-==========")
                case _ =>
                  println(s"---------- ${b1.getClass}")
              }

              val sel = Select.copy(fun)(Block(List(b1), Typed(anonTerm, interfaceType)), str)
              Block(l, TypeApply(sel, List(TypeTree.of[A])))
            case _ => null.asInstanceOf[Term]
          }
          //printTree(newBlock)
          val newDef = DefDef.copy(d)(sym, tpdef, valdef, interfaceType, Some(newBlock))
          Inlined(tree, lst, Block(List(newDef), expr))
        case _ =>
          report.error("Unexpected error")
          '{0}.asTerm
      }
      //println(newExpr)
      /*
      New(
        TypeTree[TypeRef(TermRef(ThisType(TypeRef(NoPrefix,module class java)),module lang),Object)]
      ),<init>),List()), Ident(E), Ident(DISupport)),
      */
      // TODO Create a new A with DISupport
      newExpr.asExprOf[Session => A]
    }
    else {
      '{
        { (s: Session) => s.get[A](Surface.of[A]) }
      }
    }
  }

  private def printTree(t: Any): Unit = {
    val s = new StringBuilder()
    var level = 0
    for(c <- t.toString) {
      c match {
        case '(' => 
          level += 1
          s append s"(\n${"  " * level}"
        case ')' =>
          level -= 1
          s append s"\n${"  " * level})"
        case ',' =>
          s append s",\n${"  " * level}"
        case other =>
          s += other
      }
    }
    println(s.result())
  }

}
