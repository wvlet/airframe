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

      val interfaceType = TypeTree.of[A with DISupport]
      val tm = new TreeMap {
        override def transformTerm(tree: Term)(owner: Symbol): Term = {
          println(s"Visit: ${tree.getClass}")
          tree match {
            case Ident(name) => 
              println(s"*** ${tree}")
              tree
            case tree: TypeApply =>
              println(s"==*** ${tree}")
              super.transformTerm(tree)(owner)
            case _ =>
              super.transformTerm(tree)(owner)
          }
        }
        override def transformStatement(tree: Statement)(owner: Symbol): Statement = {
          println(s"Visit statement: ${tree.getClass}")
          tree match {
            case tree: DefDef =>
              println(s"=> ${tree}")
              super.transformStatement(tree)(owner)
            case tree if tree.getClass.getName == "dotty.tools.dotc.ast.Trees$TypeDef" =>
              val td = tree.asInstanceOf[TypeDef]
              println(s"=====${td}\n-->${td.rhs}\n${td.rhs.getClass}")
              super.transformStatement(tree)(owner)
            case other =>
              super.transformStatement(tree)(owner)
          }
        }
        override def transformTypeTree(tree: TypeTree)(owner: Symbol): TypeTree = {
          tree match
            case i: TypeIdent if i.name == "LogSupport" => //=> if tree.tpe =:= TypeRepr.of[LogSupport] =>
              println(s"===: ${t} ${i.name}")
              TypeTree.of[A]
            case tree: TypeTree if tree.tpe =:= TypeRepr.of[LogSupport] => 
              println(s"-->: ${tree}, ${tree.getClass}")
              interfaceType
            case tree => 
              println(s"---: ${tree}, ${tree.getClass}")
              super.transformTypeTree(tree)(owner)
          }
      }
      
      val ds = TypeRepr.of[DISupport]

      // Create this expression:
      // '{
      //   { (s: Session) => s.getOrElse[A](Surface.of[A], new A with DISupport { def session = s }.asInsatnceOf[A]) }
      // }
      val expr = '{ (s: Session) => new LogSupport { def session = s }.asInstanceOf[LogSupport] }
      val newTree = tm.transformTerm(expr.asTerm)(Symbol.spliceOwner)
      printTree(newTree)

      //println(expr.asTerm)
      val newExpr: Term = expr.asTerm match {
        case Inlined(tree, lst, Block(List(d @ DefDef(sym, tpdef, valdef, tpt, Some(term))), expr)) => 

                    
          val newBlock: Term = term match {
            case Block(l, TypeApply(fun @ Select(Block(List(b1), b2 @ Typed(anonTerm, anonTT)), str), _)) =>
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
      newTree.asExprOf[Session => A]
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
