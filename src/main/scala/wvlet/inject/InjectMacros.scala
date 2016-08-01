package wvlet.inject

import wvlet.inject.HelixException.MISSING_CONTEXT
import wvlet.log.LogSupport
import wvlet.obj.{ObjectSchema, ObjectType}

import scala.reflect.{macros => sm}
import scala.util.Try

/**
  *
  */
object InjectMacros extends LogSupport {

  def getContext[A](enclosingObj: A): Option[Context] = {
    val cl = enclosingObj.getClass

    def returnsContext(c: Class[_]) = {
      classOf[wvlet.inject.Context].isAssignableFrom(c)
    }

    // find val or def that returns wvlet.inject.Context
    val schema = ObjectSchema(cl)

    def findContextFromMethods: Option[Context] = {
      schema
      .methods
      .find(x => returnsContext(x.valueType.rawType) && x.params.isEmpty)
      .flatMap { contextGetter =>
        Try(contextGetter.invoke(enclosingObj.asInstanceOf[AnyRef]).asInstanceOf[Context]).toOption
      }
    }

    def findContextFromParams: Option[Context] = {
      // Find parameters
      schema
      .parameters
      .find(p => returnsContext(p.valueType.rawType))
      .flatMap { contextParam => Try(contextParam.get(enclosingObj).asInstanceOf[Context]).toOption }
    }

    def findEmbeddedContext: Option[Context] = {
      // Find any embedded context
      val m = Try(cl.getDeclaredMethod("__inject_context")).toOption
      m.flatMap { m =>
        Try(m.invoke(enclosingObj).asInstanceOf[Context]).toOption
      }
    }

    findContextFromMethods
    .orElse(findContextFromParams)
    .orElse(findEmbeddedContext)
  }

  def findContext[A](enclosingObj: A): Context = {
    val cl = enclosingObj.getClass
    getContext(enclosingObj).getOrElse {
      error(s"No wvlet.inject.Context is found in the scope: ${ObjectType.of(cl)}")
      throw new HelixException(MISSING_CONTEXT(ObjectType.of(cl)))
    }
  }

  def buildImpl[A: c.WeakTypeTag](c: sm.Context)(ev: c.Tree): c.Expr[A] = {
    import c.universe._

    val t = ev.tpe.typeArgs(0)
    c.Expr(
      q"""
       wvlet.inject.InjectMacros.getContext(classOf[$t]) match {
        case Some(c) =>
           c.get(classOf[$t])
        case None =>
          new $t { protected def __inject_context = ${c.prefix} }
       }
      """
    )
  }

  def injectImpl[A: c.WeakTypeTag](c: sm.Context)(ev: c.Tree): c.Expr[A] = {
    import c.universe._

    c.Expr(
      q"""{
         val c = wvlet.inject.InjectMacros.findContext(this)
         c.get(${ev})
        }
      """
    )
  }

  def inject1Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag](c: sm.Context)(factory: c.Tree)(a: c.Tree, d1: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(
      q"""{
         val c = wvlet.inject.InjectMacros.findContext(this)
         $factory(c.get(${d1}))
        }
      """
    )
  }

  def inject2Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag](c: sm.Context)(factory: c.Tree)
                                                                         (a: c.Tree, d1: c.Tree, d2: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(q"""{ val c = wvlet.inject.InjectMacros.findContext(this); $factory(c.get(${d1}), c.get(${d2})) }""")
  }

  def inject3Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag](c: sm.Context)(factory: c.Tree)
                                                                                            (a: c.Tree, d1: c.Tree, d2: c.Tree,
                                                                                             d3: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(q"""{ val c = wvlet.inject.InjectMacros.findContext(this); $factory(c.get(${d1}), c.get(${d2}), c.get(${d3})) }""")
  }

  def inject4Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag](c: sm.Context)
                                                                                                               (factory: c.Tree)
                                                                                                               (a: c.Tree, d1: c.Tree,
                                                                                                                d2: c.Tree, d3: c.Tree,
                                                                                                                d4: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(q"""{ val c = wvlet.inject.InjectMacros.findContext(this); $factory(c.get(${d1}), c.get(${d2}), c.get(${d3}), c.get(${d4})) }""")
  }

  def inject5Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag, D5: c.WeakTypeTag](c: sm.Context)
                                                                                                                                  (factory: c.Tree)
                                                                                                                                  (a: c.Tree,
                                                                                                                                   d1: c.Tree,
                                                                                                                                   d2: c.Tree,
                                                                                                                                   d3: c.Tree,
                                                                                                                                   d4: c.Tree,
                                                                                                                                   d5: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(
      q"""{ val c = wvlet.inject.InjectMacros.findContext(this); $factory(c.get(${d1}), c.get(${d2}), c.get(${d3}), c.get(${d4}), c.get(${d5})) }""")
  }

}
