/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.di

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scala.reflect.macros.{blackbox => sm}

private[wvlet] object AirframeMacros {
  private[wvlet] class BindHelper[C <: Context](val c: C) {
    import c.universe._

    def bind(session: c.Tree, t: c.Type): c.Tree = {
      q"""{ ${newInstanceBinder(t)}(${session}) }"""
    }

    def newInstanceBinder(t: c.Type): c.Tree = {
      q"""{ session : wvlet.airframe.di.Session => session.get[$t](${surfaceOf(t)}) }"""
    }

    def createNewInstanceOf(t: c.Type): c.Tree = {
      q"""{ session : wvlet.airframe.di.Session => session.createNewInstanceOf[$t](${surfaceOf(t)}) }"""
    }

    def surfaceOf(t: c.Type): c.Tree = {
      q"wvlet.airframe.surface.Surface.of[$t]"
    }

    def provider1Binding[A: c.WeakTypeTag, D1: c.WeakTypeTag](
        factory: c.Tree,
        singleton: Boolean,
        eager: Boolean
    ): c.Tree = {
      val t   = implicitly[c.WeakTypeTag[A]].tpe
      val ev1 = implicitly[c.WeakTypeTag[D1]].tpe
      q"""{
           val self = ${c.prefix.tree}
           val d1 = ${surfaceOf(ev1)}
           import wvlet.airframe.di.Binder._
           self.design.addBinding[${t}](ProviderBinding(DependencyFactory(self.from, Seq(d1), ${factory}), ${singleton}, ${eager}, self.sourceCode))
        }
        """
    }

    def provider2Binding[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag](
        factory: c.Tree,
        singleton: Boolean,
        eager: Boolean
    ): c.Tree = {
      val t   = implicitly[c.WeakTypeTag[A]].tpe
      val ev1 = implicitly[c.WeakTypeTag[D1]].tpe
      val ev2 = implicitly[c.WeakTypeTag[D2]].tpe
      q"""{
           val self = ${c.prefix.tree}
           val d1 = ${surfaceOf(ev1)}
           val d2 = ${surfaceOf(ev2)}
           import wvlet.airframe.di.Binder._
           self.design.addBinding[${t}](ProviderBinding(DependencyFactory(self.from, Seq(d1, d2), ${factory}), ${singleton}, ${eager}, self.sourceCode))
        }
        """
    }

    def provider3Binding[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag](
        factory: c.Tree,
        singleton: Boolean,
        eager: Boolean
    ): c.Tree = {
      val t   = implicitly[c.WeakTypeTag[A]].tpe
      val ev1 = implicitly[c.WeakTypeTag[D1]].tpe
      val ev2 = implicitly[c.WeakTypeTag[D2]].tpe
      val ev3 = implicitly[c.WeakTypeTag[D3]].tpe
      q"""{
           val self = ${c.prefix.tree}
           val d1 = ${surfaceOf(ev1)}
           val d2 = ${surfaceOf(ev2)}
           val d3 = ${surfaceOf(ev3)}
           import wvlet.airframe.di.Binder._
           self.design.addBinding[${t}](ProviderBinding(DependencyFactory(self.from, Seq(d1, d2, d3), ${factory}), ${singleton}, ${eager}, self.sourceCode))
        }
        """
    }

    def provider4Binding[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag](
        factory: c.Tree,
        singleton: Boolean,
        eager: Boolean
    ): c.Tree = {
      val t   = implicitly[c.WeakTypeTag[A]].tpe
      val ev1 = implicitly[c.WeakTypeTag[D1]].tpe
      val ev2 = implicitly[c.WeakTypeTag[D2]].tpe
      val ev3 = implicitly[c.WeakTypeTag[D3]].tpe
      val ev4 = implicitly[c.WeakTypeTag[D4]].tpe
      q"""{
           val self = ${c.prefix.tree}
           val d1 = ${surfaceOf(ev1)}
           val d2 = ${surfaceOf(ev2)}
           val d3 = ${surfaceOf(ev3)}
           val d4 = ${surfaceOf(ev4)}
           import wvlet.airframe.di.Binder._
           self.design.addBinding[${t}](ProviderBinding(DependencyFactory(self.from, Seq(d1, d2, d3, d4), ${factory}), ${singleton}, ${eager}, self.sourceCode))
        }
        """
    }

    def provider5Binding[
        A: c.WeakTypeTag,
        D1: c.WeakTypeTag,
        D2: c.WeakTypeTag,
        D3: c.WeakTypeTag,
        D4: c.WeakTypeTag,
        D5: c.WeakTypeTag
    ](
        factory: c.Tree,
        singleton: Boolean,
        eager: Boolean
    ): c.Tree = {
      val t   = implicitly[c.WeakTypeTag[A]].tpe
      val ev1 = implicitly[c.WeakTypeTag[D1]].tpe
      val ev2 = implicitly[c.WeakTypeTag[D2]].tpe
      val ev3 = implicitly[c.WeakTypeTag[D3]].tpe
      val ev4 = implicitly[c.WeakTypeTag[D4]].tpe
      val ev5 = implicitly[c.WeakTypeTag[D5]].tpe
      q"""{
           val self = ${c.prefix.tree}
           val d1 = ${surfaceOf(ev1)}
           val d2 = ${surfaceOf(ev2)}
           val d3 = ${surfaceOf(ev3)}
           val d4 = ${surfaceOf(ev4)}
           val d5 = ${surfaceOf(ev5)}
           import wvlet.airframe.di.Binder._
           self.design.addBinding[${t}](ProviderBinding(DependencyFactory(self.from, Seq(d1, d2, d3, d4, d5), ${factory}), ${singleton}, ${eager}, self.sourceCode))
        }
        """
    }

    def fullTypeNameOf(typeEv: c.Type): String = {
      typeEv match {
        case TypeRef(prefix, typeSymbol, args) =>
          if (args.isEmpty) {
            typeSymbol.fullName
          } else {
            val typeArgs = args.map(fullTypeNameOf(_)).mkString(",")
            s"${typeSymbol.fullName}[${typeArgs}]"
          }
        case other =>
          typeEv.typeSymbol.fullName
      }
    }
  }

  def designBindImpl[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    q"""{
         val __surface = ${h.surfaceOf(t)}
         new wvlet.airframe.di.Binder(${c.prefix}, __surface, ${sourceCode(c)}).asInstanceOf[wvlet.airframe.di.Binder[$t]]
    }"""
  }

  def designRemoveImpl[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    q"""{
         val d = ${c.prefix}
         val target = ${h.surfaceOf(t)}
         new wvlet.airframe.di.Design(d.designOptions, d.binding.filterNot(_.from == target), d.hooks)
        }
     """
  }

  def binderToImpl[B: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[B]].tpe
    val h = new BindHelper[c.type](c)
    q""" {
      val self = ${c.prefix}
      val to = ${h.surfaceOf(t)}
      self.design.addBinding[${t}](wvlet.airframe.di.Binder.ClassBinding(self.from, to, self.sourceCode))
    }"""
  }

  def binderToSingletonOfImpl[B: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[B]].tpe
    val h = new BindHelper[c.type](c)
    q""" {
      val self = ${c.prefix.tree}
      val to = ${h.surfaceOf(t)}
      if(self.from == to) {
         wvlet.log.Logger("wvlet.airframe.di.Binder").warn("Binding to the same type is not allowed: " + to.toString)
         throw new wvlet.airframe.di.DIException.CYCLIC_DEPENDENCY(List(to), ${sourceCode(c)})
      }
      self.design.addBinding[${t}](wvlet.airframe.di.Binder.SingletonBinding(self.from, to, false, self.sourceCode))
    }"""
  }

  def binderToEagerSingletonOfImpl[B: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[B]].tpe
    val h = new BindHelper[c.type](c)
    q""" {
      val self = ${c.prefix.tree}
      val to = ${h.surfaceOf(t)}
      if(self.from == to) {
         wvlet.log.Logger("wvlet.airframe.di.Binder").warn("Binding to the same type is not allowed: " + to.toString)
         throw new wvlet.airframe.di.DIException.CYCLIC_DEPENDENCY(List(to), ${sourceCode(c)})
      }
      self.design.addBinding[${t}](wvlet.airframe.di.Binder.SingletonBinding(self.from, to, true, self.sourceCode))
    }"""
  }

  def bindToSingletonProvider1[A: c.WeakTypeTag, D1: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider1Binding[A, D1](factory, true, false)
  }

  def bindToSingletonProvider2[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag](
      c: sm.Context
  )(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider2Binding[A, D1, D2](factory, true, false)
  }

  def bindToSingletonProvider3[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag](
      c: sm.Context
  )(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider3Binding[A, D1, D2, D3](factory, true, false)
  }

  def bindToSingletonProvider4[
      A: c.WeakTypeTag,
      D1: c.WeakTypeTag,
      D2: c.WeakTypeTag,
      D3: c.WeakTypeTag,
      D4: c.WeakTypeTag
  ](
      c: sm.Context
  )(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider4Binding[A, D1, D2, D3, D4](factory, true, false)
  }

  def bindToSingletonProvider5[
      A: c.WeakTypeTag,
      D1: c.WeakTypeTag,
      D2: c.WeakTypeTag,
      D3: c.WeakTypeTag,
      D4: c.WeakTypeTag,
      D5: c.WeakTypeTag
  ](c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider5Binding[A, D1, D2, D3, D4, D5](factory, true, false)
  }

  def bindToEagerSingletonProvider1[A: c.WeakTypeTag, D1: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider1Binding[A, D1](factory, true, true)
  }

  def bindToEagerSingletonProvider2[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag](
      c: sm.Context
  )(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider2Binding[A, D1, D2](factory, true, true)
  }

  def bindToEagerSingletonProvider3[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag](
      c: sm.Context
  )(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider3Binding[A, D1, D2, D3](factory, true, true)
  }

  def bindToEagerSingletonProvider4[
      A: c.WeakTypeTag,
      D1: c.WeakTypeTag,
      D2: c.WeakTypeTag,
      D3: c.WeakTypeTag,
      D4: c.WeakTypeTag
  ](
      c: sm.Context
  )(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider4Binding[A, D1, D2, D3, D4](factory, true, true)
  }

  def bindToEagerSingletonProvider5[
      A: c.WeakTypeTag,
      D1: c.WeakTypeTag,
      D2: c.WeakTypeTag,
      D3: c.WeakTypeTag,
      D4: c.WeakTypeTag,
      D5: c.WeakTypeTag
  ](c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider5Binding[A, D1, D2, D3, D4, D5](factory, true, true)
  }

  /**
    * Used when Session location is known
    *
    * @param c
    * @tparam A
    * @return
    */
  def buildImpl[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    val t = implicitly[c.WeakTypeTag[A]].tpe
    new BindHelper[c.type](c).bind(c.prefix.tree, t)
  }

  def buildWithSession[A: c.WeakTypeTag](c: sm.Context)(body: c.Expr[A => Any]): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    // Bind the code block to a local variable to resolve the issue #373
    val e = q"""
         {
           val codeBlock = ${body}
           (${c.prefix}).withSession { session =>
              val a = session.build[${t}]
              codeBlock(a)
           }
         }
     """
    e
  }

  def runWithSession[A: c.WeakTypeTag, B: c.WeakTypeTag](c: sm.Context)(body: c.Expr[A => B]): c.Tree = {
    import c.universe._
    val a = implicitly[c.WeakTypeTag[A]].tpe
    val b = implicitly[c.WeakTypeTag[B]].tpe
    // Bind the code block to a local variable to resolve the issue #373
    val e = q"""
         {
           val codeBlock = ${body}
           (${c.prefix}).withSession { session =>
              val a = session.build[${a}]
              codeBlock(a)
           }
         }.asInstanceOf[${b}]
     """
    e
  }

  def sourceCode(c: sm.Context): c.Tree = {
    import c.universe._
    c.internal.enclosingOwner
    val pos = c.enclosingPosition
    q"wvlet.airframe.di.SourceCode(${pos.source.path}, ${pos.source.file.name}, ${pos.line}, ${pos.column})"
  }
}
