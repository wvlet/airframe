package wvlet.inject

import wvlet.inject.InjectionException.MISSING_CONTEXT
import wvlet.log.LogSupport
import wvlet.obj.{ObjectSchema, ObjectType}

import scala.reflect.{macros => sm}
import scala.util.Try

/**
  *
  */
object InjectMacros extends LogSupport {

  def findContextAccess[A](cl: Class[A]): Option[AnyRef => Context] = {

    info(s"Find context for ${cl}")

    def returnsContext(c: Class[_]) = {
      classOf[wvlet.inject.Context].isAssignableFrom(c)
    }

    // find val or def that returns wvlet.inject.Context
    val schema = ObjectSchema(cl)

    def findContextFromMethods: Option[AnyRef => Context] =
      schema
      .methods
      .find(x => returnsContext(x.valueType.rawType) && x.params.isEmpty)
      .map { contextGetter => { obj: AnyRef => contextGetter.invoke(obj).asInstanceOf[Context] }
      }

    def findContextFromParams: Option[AnyRef => Context] = {
      // Find parameters
      schema
      .parameters
      .find(p => returnsContext(p.valueType.rawType))
      .map { contextParam => { obj: AnyRef => contextParam.get(obj).asInstanceOf[Context] } }
    }

    def findEmbeddedContext: Option[AnyRef => Context] = {
      // Find any embedded context
      val m = Try(cl.getDeclaredMethod("__inject_context")).toOption
      m.map { m => { obj: AnyRef => m.invoke(obj).asInstanceOf[Context] }
      }
    }

    findContextFromMethods
    .orElse(findContextFromParams)
    .orElse(findEmbeddedContext)
  }

  def getContext[A](enclosingObj: A): Option[Context] = {
    require(enclosingObj != null, "enclosinbObj is null")
    findContextAccess(enclosingObj.getClass).flatMap { access =>
      Try(access.apply(enclosingObj.asInstanceOf[AnyRef])).toOption
    }
  }

  def findContext[A](enclosingObj: A): Context = {
    val cl = enclosingObj.getClass
    getContext(enclosingObj).getOrElse {
      error(s"No wvlet.inject.Context is found in the scope: ${ObjectType.of(cl)}")
      throw new InjectionException(MISSING_CONTEXT(ObjectType.of(cl)))
    }
  }


//  def getImpl[A: c.WeakTypeTag](c: sm.Context)(ev: c.Tree): c.Tree = {
//    import c.universe._
//    val t = q"${ev.tpe.typeArgs(0)}"
//    q"""
//       {
//        val tpe = wvlet.obj.ObjectType.ofTypeTag($ev)
//        wvlet.inject.InjectMacros.findContextAccess(this.getClass) match {
//           case Some(x) =>
//              ${c.prefix}.newInstance(wvlet.obj.ObjectType.of($ev.tpe), Set.empty).asInstanceOf[$t]
//           case None =>
//              throw new IllegalStateException(s"No context is found for $$tpe")
//        }
//       }
//     """
//  }

  def buildImpl[A: c.WeakTypeTag](c: sm.Context)(ev: c.Tree): c.Expr[A] = {
    import c.universe._

    val t = ev.tpe
    c.Expr(
//      q"""{
//       val t = wvlet.obj.ObjectType.of(${ev}.tpe)
//       wvlet.inject.InjectMacros.findContextAccess(t.rawType) match {
//        case Some(access) =>
//           access(this.asInstanceOf[AnyRef]).get(cl)($t)
//        case None =>
//         new ${t.typeArgs(0)} { protected def __inject_context = ${c.prefix} }
//       }
//      }
//      """
     q"""new ${ev.tpe.typeArgs(0)} { protected def __inject_context = ${c.prefix} }"""
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
         c.getOrElseUpdate($factory(c.get(${d1})))
        }
      """
    )
  }

  def inject2Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(q"""{ val c = wvlet.inject.InjectMacros.findContext(this); c.getOrElseUpdate($factory(c.get(${d1}), c.get(${d2}))) }""")
  }

  def inject3Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree, d3: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(q"""{ val c = wvlet.inject.InjectMacros.findContext(this); c.getOrElseUpdate($factory(c.get(${d1}), c.get(${d2}), c.get(${d3}))) }""")
  }

  def inject4Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree, d3: c.Tree, d4: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(q"""{ val c = wvlet.inject.InjectMacros.findContext(this); c.getOrElseUpdate($factory(c.get(${d1}), c.get(${d2}), c.get(${d3}), c.get(${d4}))) }""")
  }

  def inject5Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag, D5: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree, d3: c.Tree, d4: c.Tree, d5: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(
      q"""{ val c = wvlet.inject.InjectMacros.findContext(this); c.getOrElseUpdate($factory(c.get(${d1}), c.get(${d2}), c.get(${d3}), c.get(${d4}), c.get(${d5}))) }""")
  }

}
