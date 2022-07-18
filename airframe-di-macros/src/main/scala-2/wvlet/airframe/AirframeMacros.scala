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
package wvlet.airframe

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scala.reflect.macros.{blackbox => sm}

private[wvlet] object AirframeMacros {
  private[wvlet] class BindHelper[C <: Context](val c: C) {
    import c.universe._

    def shouldGenerateTrait(t: c.Type): Boolean = {
      val a = t.typeSymbol

      // Find the public default constructor that has no arguments
      val hasPublicDefaultConstructor = t.members
        .find(_.isConstructor)
        .map(_.asMethod).exists { m => m.isPublic && m.paramLists.size == 1 && m.paramLists(0).size == 0 }

      val hasAbstractMethods = t.members.exists(x => x.isMethod && x.isAbstract && !x.isAbstractOverride)

      val isTaggedType = t.typeSymbol.fullName.startsWith("wvlet.airframe.surface.tag.")

      val isSealedType = t.typeSymbol.isClass && t.typeSymbol.asClass.isSealed

      val shouldInstantiateTrait = if (!a.isStatic) {
        // = Non static type
        // If X is non static type (= local class or trait),
        // we need to instantiate it first in order to populate its $outer variables

        // We cannot instantiate path-dependent types
        if (t.toString.contains("#")) {
          false
        } else {
          hasPublicDefaultConstructor
        }
      } else if (a.isAbstract) {
        // = Abstract type
        // We cannot build abstract type X that has abstract methods, so bind[X].to[ConcreteType]
        // needs to be found in the design

        // If there is no abstract methods, it might be a trait without any method
        !hasAbstractMethods
      } else {
        // We cannot instantiate any trait or class without the default constructor
        // So binding needs to be found in the Design.
        hasPublicDefaultConstructor
      }

      // Tagged type or sealed class binding should be found in Design
      !isTaggedType && !isSealedType && shouldInstantiateTrait
    }

    def bind(session: c.Tree, t: c.Type): c.Tree = {
      q"""{
            val session = ${session}
            ${newInstanceBinder(t)}(session)
          }"""
    }

    def findSession: c.Tree = {
      q"""wvlet.airframe.Session.findSession(this)"""
    }

    def newInstanceBinder(t: c.Type): c.Tree = {
      if (shouldGenerateTrait(t)) {
        q"""{
             ss : wvlet.airframe.Session =>
             ss.getOrElse(${surfaceOf(t)},
              (new $t with wvlet.airframe.DISupport { def session = ss }).asInstanceOf[$t]
             )
            }"""
      } else {
        q"""{ session : wvlet.airframe.Session => session.get[$t](${surfaceOf(t)}) }"""
      }
    }

    def createNewInstanceOf(t: c.Type): c.Tree = {
      if (shouldGenerateTrait(t)) {
        q"""{
             ss : wvlet.airframe.Session =>
             ss.createNewInstanceOf(${surfaceOf(t)},
              (new $t with wvlet.airframe.DISupport { def session = ss }).asInstanceOf[$t]
             )
            }"""
      } else {
        q"""{ session : wvlet.airframe.Session => session.createNewInstanceOf[$t](${surfaceOf(t)}) }"""
      }
    }

    /**
      * Register a factory for generating a trait that can embed the current session. This step is necessary for
      * instantiating trait, which has no default constructor.
      *
      * This method will return the surface of t
      */
    def registerTraitFactory(t: c.Type): c.Tree = {
      if (shouldGenerateTrait(t)) {
        q""" {
           val s = ${surfaceOf(t)}
           wvlet.airframe.getOrElseUpdateTraitFactoryCache(s,
             { ss: wvlet.airframe.Session => (new $t with wvlet.airframe.DISupport { def session = ss }).asInstanceOf[Any] }
           )
           s
         }
        """
      } else {
        q"""{${surfaceOf(t)}}"""
      }
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
           val d1 = ${registerTraitFactory(ev1)}
           import wvlet.airframe.Binder._
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
           val d1 = ${registerTraitFactory(ev1)}
           val d2 = ${registerTraitFactory(ev2)}
           import wvlet.airframe.Binder._
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
           val d1 = ${registerTraitFactory(ev1)}
           val d2 = ${registerTraitFactory(ev2)}
           val d3 = ${registerTraitFactory(ev3)}
           import wvlet.airframe.Binder._
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
           val d1 = ${registerTraitFactory(ev1)}
           val d2 = ${registerTraitFactory(ev2)}
           val d3 = ${registerTraitFactory(ev3)}
           val d4 = ${registerTraitFactory(ev4)}
           import wvlet.airframe.Binder._
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
           val d1 = ${registerTraitFactory(ev1)}
           val d2 = ${registerTraitFactory(ev2)}
           val d3 = ${registerTraitFactory(ev3)}
           val d4 = ${registerTraitFactory(ev4)}
           val d5 = ${registerTraitFactory(ev5)}
           import wvlet.airframe.Binder._
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

  def registerTraitFactoryImpl[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    h.registerTraitFactory(t)
  }

  def designBindImpl[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    q"""{
         val __surface = ${h.registerTraitFactory(t)}
         new wvlet.airframe.Binder(${c.prefix}, __surface, ${sourceCode(c)}).asInstanceOf[wvlet.airframe.Binder[$t]]
    }"""
  }

  def designRemoveImpl[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    q"""{
         val d = ${c.prefix}
         val target = ${h.surfaceOf(t)}
         new wvlet.airframe.Design(d.designOptions, d.binding.filterNot(_.from == target), d.hooks)
        }
     """
  }

  def binderToImpl[B: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[B]].tpe
    val h = new BindHelper[c.type](c)
    q""" {
      val self = ${c.prefix}
      val to = ${h.registerTraitFactory(t)}
      self.design.addBinding[${t}](wvlet.airframe.Binder.ClassBinding(self.from, to, self.sourceCode))
    }"""
  }

  def binderToSingletonOfImpl[B: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[B]].tpe
    val h = new BindHelper[c.type](c)
    q""" {
      val self = ${c.prefix.tree}
      val to = ${h.registerTraitFactory(t)}
      if(self.from == to) {
         wvlet.log.Logger("wvlet.airframe.Binder").warn("Binding to the same type is not allowed: " + to.toString)
         throw new wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY(List(to), ${sourceCode(c)})
      }
      self.design.addBinding[${t}](wvlet.airframe.Binder.SingletonBinding(self.from, to, false, self.sourceCode))
    }"""
  }

  def binderToEagerSingletonOfImpl[B: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[B]].tpe
    val h = new BindHelper[c.type](c)
    q""" {
      val self = ${c.prefix.tree}
      val to = ${h.registerTraitFactory(t)}
      if(self.from == to) {
         wvlet.log.Logger("wvlet.airframe.Binder").warn("Binding to the same type is not allowed: " + to.toString)
         throw new wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY(List(to), ${sourceCode(c)})
      }
      self.design.addBinding[${t}](wvlet.airframe.Binder.SingletonBinding(self.from, to, true, self.sourceCode))
    }"""
  }

  def bindToProvider1[A: c.WeakTypeTag, D1: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider1Binding[A, D1](factory, false, false)
  }

  def bindToProvider2[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag](
      c: sm.Context
  )(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider2Binding[A, D1, D2](factory, false, false)
  }

  def bindToProvider3[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag](
      c: sm.Context
  )(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider3Binding[A, D1, D2, D3](factory, false, false)
  }

  def bindToProvider4[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag](
      c: sm.Context
  )(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider4Binding[A, D1, D2, D3, D4](factory, false, false)
  }

  def bindToProvider5[
      A: c.WeakTypeTag,
      D1: c.WeakTypeTag,
      D2: c.WeakTypeTag,
      D3: c.WeakTypeTag,
      D4: c.WeakTypeTag,
      D5: c.WeakTypeTag
  ](
      c: sm.Context
  )(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider5Binding[A, D1, D2, D3, D4, D5](factory, false, false)
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

  def addLifeCycle[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._

    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    q"""{
         val session = ${h.findSession}
         new wvlet.airframe.LifeCycleBinder(${c.prefix}.dep, ${h.surfaceOf(t)}, session)
        }
      """
  }

  def addInitLifeCycle[A: c.WeakTypeTag](c: sm.Context)(body: c.Tree): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    q"""{
         val session = ${h.findSession}
         val dep = ${c.prefix}.dep
         session.lifeCycleManager.addInitHook(wvlet.airframe.lifecycle.EventHookHolder(${h.surfaceOf(t)}, dep, ${body}))
         dep
        }
      """
  }

  def addInjectLifeCycle[A: c.WeakTypeTag](c: sm.Context)(body: c.Tree): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    q"""{
         val session = ${h.findSession}
         val dep = ${c.prefix}.dep
         session.lifeCycleManager.addInjectHook(wvlet.airframe.lifecycle.EventHookHolder(${h
        .surfaceOf(t)}, dep, ${body}))
         dep
        }
      """
  }

  def addStartLifeCycle[A: c.WeakTypeTag](c: sm.Context)(body: c.Tree): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    q"""{
         val session = ${h.findSession}
         val dep = ${c.prefix}.dep
         session.lifeCycleManager.addStartHook(wvlet.airframe.lifecycle.EventHookHolder(${h
        .surfaceOf(t)}, dep, ${body}))
         dep
        }
      """
  }

  def addStartLifeCycleForFactory[F: c.WeakTypeTag](c: sm.Context)(body: c.Tree): c.Tree = {
    import c.universe._
    val t  = implicitly[c.WeakTypeTag[F]].tpe
    val i1 = t.typeArgs(0)
    val a  = t.typeArgs(1)
    val h  = new BindHelper[c.type](c)
    q"""{
         val session = ${h.findSession}
         val dep = ${c.prefix}.dep
         session.lifeCycleManager.addStartHook(wvlet.airframe.lifecycle.EventHookHolder(${h
        .surfaceOf(t)}, dep, ${body}))
         dep
        }
      """
  }

  def addPreShutdownLifeCycle[A: c.WeakTypeTag](c: sm.Context)(body: c.Tree): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    q"""{
         val session = ${h.findSession}
         val dep = ${c.prefix}.dep
         session.lifeCycleManager.addPreShutdownHook(wvlet.airframe.lifecycle.EventHookHolder(${h
        .surfaceOf(t)}, dep, ${body}))
         dep
        }
      """
  }

  def addShutdownLifeCycle[A: c.WeakTypeTag](c: sm.Context)(body: c.Tree): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    q"""{
         val session = ${h.findSession}
         val dep = ${c.prefix}.dep
         session.lifeCycleManager.addShutdownHook(wvlet.airframe.lifecycle.EventHookHolder(${h
        .surfaceOf(t)}, dep, ${body}))
         dep
        }
      """
  }

  def bindImpl[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    h.bind(h.findSession, t)
  }

  def bind0Impl[A: c.WeakTypeTag](c: sm.Context)(provider: c.Tree): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    q"""{
         val session = ${h.findSession}
         session.getOrElse(${h.surfaceOf(t)}, $provider)
        }
      """
  }

  def bind1Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag](c: sm.Context)(provider: c.Tree): c.Tree = {
    import c.universe._
    val h    = new BindHelper[c.type](c)
    val t    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val dep1 = h.newInstanceBinder(d1)
    q"""{
         val session = ${h.findSession}
         session.getOrElse(${h.surfaceOf(t)}, $provider($dep1(session)))
        }
      """
  }

  def bind2Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag](c: sm.Context)(provider: c.Tree): c.Tree = {
    import c.universe._
    val h    = new BindHelper[c.type](c)
    val t    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val dep1 = h.newInstanceBinder(d1)
    val dep2 = h.newInstanceBinder(d2)
    q"""{
         val session = ${h.findSession}
         session.getOrElse(${h.surfaceOf(t)}, $provider($dep1(session), $dep2(session)))
        }
      """
  }

  def bind3Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag](
      c: sm.Context
  )(provider: c.Tree): c.Tree = {
    import c.universe._
    val h    = new BindHelper[c.type](c)
    val t    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val d3   = implicitly[c.WeakTypeTag[D3]].tpe
    val dep1 = h.newInstanceBinder(d1)
    val dep2 = h.newInstanceBinder(d2)
    val dep3 = h.newInstanceBinder(d3)
    q"""{
         val session = ${h.findSession}
         session.getOrElse(${h.surfaceOf(t)}, $provider($dep1(session),$dep2(session),$dep3(session)))
        }
      """
  }

  def bind4Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag](
      c: sm.Context
  )(provider: c.Tree): c.Tree = {
    import c.universe._
    val h    = new BindHelper[c.type](c)
    val t    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val d3   = implicitly[c.WeakTypeTag[D3]].tpe
    val d4   = implicitly[c.WeakTypeTag[D4]].tpe
    val dep1 = h.newInstanceBinder(d1)
    val dep2 = h.newInstanceBinder(d2)
    val dep3 = h.newInstanceBinder(d3)
    val dep4 = h.newInstanceBinder(d4)
    q"""{
         val session = ${h.findSession}
         session.getOrElse(${h.surfaceOf(t)},
           $provider($dep1(session),$dep2(session),$dep3(session),$dep4(session))
         )
        }
      """
  }

  def bind5Impl[
      A: c.WeakTypeTag,
      D1: c.WeakTypeTag,
      D2: c.WeakTypeTag,
      D3: c.WeakTypeTag,
      D4: c.WeakTypeTag,
      D5: c.WeakTypeTag
  ](c: sm.Context)(provider: c.Tree): c.Tree = {
    import c.universe._
    val h    = new BindHelper[c.type](c)
    val t    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val d3   = implicitly[c.WeakTypeTag[D3]].tpe
    val d4   = implicitly[c.WeakTypeTag[D4]].tpe
    val d5   = implicitly[c.WeakTypeTag[D5]].tpe
    val dep1 = h.newInstanceBinder(d1)
    val dep2 = h.newInstanceBinder(d2)
    val dep3 = h.newInstanceBinder(d3)
    val dep4 = h.newInstanceBinder(d4)
    val dep5 = h.newInstanceBinder(d5)
    q"""{
         val session = ${h.findSession}
         session.getOrElse(${h.surfaceOf(t)},
           $provider($dep1(session),$dep2(session),$dep3(session),$dep4(session),$dep5(session))
         )
        }
      """
  }

  def bindLocal0Impl[A: c.WeakTypeTag](c: sm.Context)(provider: c.Tree): c.Tree = {
    import c.universe._

    val a = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    q"""{
          val surface = ${h.surfaceOf(a)}
          val session = ${h.findSession}
          val newChildDesign = wvlet.airframe.newDesign.bind(surface).toLazyInstance(${provider})
          val localSession = session.newSharedChildSession(newChildDesign)
          localSession.get[$a](surface)
        }
      """
  }

  def bindLocal1Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag](c: sm.Context)(provider: c.Tree): c.Tree = {
    import c.universe._

    val a    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val h    = new BindHelper[c.type](c)
    val dep1 = h.newInstanceBinder(d1)
    q"""{
          val surface = ${h.surfaceOf(a)}
          val session = ${h.findSession}
          val newChildDesign = wvlet.airframe.newDesign.bind(surface).toLazyInstance($provider($dep1(session)))
          val localSession = session.newSharedChildSession(newChildDesign)
          localSession.get[$a](surface)
        }
      """
  }

  def bindLocal2Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag](
      c: sm.Context
  )(provider: c.Tree): c.Tree = {
    import c.universe._

    val a    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val h    = new BindHelper[c.type](c)
    val dep1 = h.newInstanceBinder(d1)
    val dep2 = h.newInstanceBinder(d2)
    q"""{
          val surface = ${h.surfaceOf(a)}
          val session = ${h.findSession}
          val newChildDesign = wvlet.airframe.newDesign.bind(surface).toLazyInstance($provider($dep1(session), $dep2(session)))
          val localSession = session.newSharedChildSession(newChildDesign)
          localSession.get[$a](surface)
        }
      """
  }

  def bindLocal3Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag](
      c: sm.Context
  )(provider: c.Tree): c.Tree = {
    import c.universe._

    val a    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val d3   = implicitly[c.WeakTypeTag[D3]].tpe
    val h    = new BindHelper[c.type](c)
    val dep1 = h.newInstanceBinder(d1)
    val dep2 = h.newInstanceBinder(d2)
    val dep3 = h.newInstanceBinder(d3)
    q"""{
          val surface = ${h.surfaceOf(a)}
          val session = ${h.findSession}
          val newChildDesign = wvlet.airframe.newDesign.bind(surface).toLazyInstance($provider($dep1(session), $dep2(session), $dep3(session)))
          val localSession = session.newSharedChildSession(newChildDesign)
          localSession.get[$a](surface)
        }
      """
  }

  def bindLocal4Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag](
      c: sm.Context
  )(provider: c.Tree): c.Tree = {
    import c.universe._

    val a    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val d3   = implicitly[c.WeakTypeTag[D3]].tpe
    val d4   = implicitly[c.WeakTypeTag[D4]].tpe
    val h    = new BindHelper[c.type](c)
    val dep1 = h.newInstanceBinder(d1)
    val dep2 = h.newInstanceBinder(d2)
    val dep3 = h.newInstanceBinder(d3)
    val dep4 = h.newInstanceBinder(d4)
    q"""{
          val surface = ${h.surfaceOf(a)}
          val session = ${h.findSession}
          val newChildDesign = wvlet.airframe.newDesign.bind(surface).toLazyInstance($provider($dep1(session), $dep2(session), $dep3(session), $dep4(session)))
          val localSession = session.newSharedChildSession(newChildDesign)
          localSession.get[$a](surface)
        }
      """
  }

  def bindLocal5Impl[
      A: c.WeakTypeTag,
      D1: c.WeakTypeTag,
      D2: c.WeakTypeTag,
      D3: c.WeakTypeTag,
      D4: c.WeakTypeTag,
      D5: c.WeakTypeTag
  ](
      c: sm.Context
  )(provider: c.Tree): c.Tree = {
    import c.universe._

    val a    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val d3   = implicitly[c.WeakTypeTag[D3]].tpe
    val d4   = implicitly[c.WeakTypeTag[D4]].tpe
    val d5   = implicitly[c.WeakTypeTag[D5]].tpe
    val h    = new BindHelper[c.type](c)
    val dep1 = h.newInstanceBinder(d1)
    val dep2 = h.newInstanceBinder(d2)
    val dep3 = h.newInstanceBinder(d3)
    val dep4 = h.newInstanceBinder(d4)
    val dep5 = h.newInstanceBinder(d5)
    q"""{
        val surface = ${h.surfaceOf(a)}
        val session = ${h.findSession}
        val newChildDesign = wvlet.airframe.newDesign.bind(surface).toLazyInstance($provider($dep1(session), $dep2(session), $dep3(session), $dep4(session), $dep5(session)))
        val localSession = session.newSharedChildSession(newChildDesign)
        localSession.get[$a](surface)
      }
    """
  }

  def bindFactoryImpl[F: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._

    import scala.language.higherKinds
    val t  = implicitly[c.WeakTypeTag[F]].tpe // F = Function[I1, A]
    val i1 = t.typeArgs(0)                    // I1
    val a  = t.typeArgs(1)                    // A
    val h  = new BindHelper[c.type](c)
    q"""{ i1: ${i1} =>
          val session = ${h.findSession}.newSharedChildSession(wvlet.airframe.newDesign.bind(${h
        .surfaceOf(i1)}).toLazyInstance(i1))
          ${h.createNewInstanceOf(a)}(session)
        }
      """
  }

  def bindFactory2Impl[F: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._

    import scala.language.higherKinds
    val t  = implicitly[c.WeakTypeTag[F]].tpe // F = Function[(I1, I2), A]
    val i1 = t.typeArgs(0)                    // I1
    val i2 = t.typeArgs(1)                    // I2
    val a  = t.typeArgs(2)                    // A
    val h  = new BindHelper[c.type](c)
    q"""{ (i1: ${i1}, i2: ${i2}) =>
         val session = ${h.findSession}.newSharedChildSession(
           wvlet.airframe.newDesign
           .bind(${h.surfaceOf(i1)}).toLazyInstance(i1)
           .bind(${h.surfaceOf(i2)}).toLazyInstance(i2)
         )
         ${h.createNewInstanceOf(a)}(session)
        }
      """
  }

  def bindFactory3Impl[F: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._

    import scala.language.higherKinds
    val t  = implicitly[c.WeakTypeTag[F]].tpe // F = Function[(I1, I2, I3), A]
    val i1 = t.typeArgs(0)                    // I1
    val i2 = t.typeArgs(1)                    // I2
    val i3 = t.typeArgs(2)                    // I3
    val a  = t.typeArgs(3)                    // A
    val h  = new BindHelper[c.type](c)
    q"""{ (i1: ${i1}, i2: ${i2}, i3:${i3}) =>
         val session = ${h.findSession}.newSharedChildSession(
           wvlet.airframe.newDesign
           .bind(${h.surfaceOf(i1)}).toLazyInstance(i1)
           .bind(${h.surfaceOf(i2)}).toLazyInstance(i2)
           .bind(${h.surfaceOf(i3)}).toLazyInstance(i3)
         )
         ${h.createNewInstanceOf(a)}(session)
        }
      """
  }

  def bindFactory4Impl[F: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._

    import scala.language.higherKinds
    val t  = implicitly[c.WeakTypeTag[F]].tpe // F = Function[(I1, I2, I3, I4), A]
    val i1 = t.typeArgs(0)                    // I1
    val i2 = t.typeArgs(1)                    // I2
    val i3 = t.typeArgs(2)                    // I3
    val i4 = t.typeArgs(3)                    // I4
    val a  = t.typeArgs(4)                    // A
    val h  = new BindHelper[c.type](c)
    q"""{ (i1: ${i1}, i2: ${i2}, i3:${i3}, i4:${i4}) =>
         val session = ${h.findSession}.newSharedChildSession(
           wvlet.airframe.newDesign
           .bind(${h.surfaceOf(i1)}).toLazyInstance(i1)
           .bind(${h.surfaceOf(i2)}).toLazyInstance(i2)
           .bind(${h.surfaceOf(i3)}).toLazyInstance(i3)
           .bind(${h.surfaceOf(i4)}).toLazyInstance(i4)
         )
         ${h.createNewInstanceOf(a)}(session)
        }
      """
  }

  def bindFactory5Impl[F: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._

    import scala.language.higherKinds
    val t  = implicitly[c.WeakTypeTag[F]].tpe // F = Function[(I1, I2, I3, I4, I4), A]
    val i1 = t.typeArgs(0)                    // I1
    val i2 = t.typeArgs(1)                    // I2
    val i3 = t.typeArgs(2)                    // I3
    val i4 = t.typeArgs(3)                    // I4
    val i5 = t.typeArgs(4)                    // I5
    val a  = t.typeArgs(5)                    // A
    val h  = new BindHelper[c.type](c)
    q"""{ (i1: ${i1}, i2: ${i2}, i3:${i3}, i4:${i4}, i5:${i5}) =>
         val session = ${h.findSession}.newSharedChildSession(
           wvlet.airframe.newDesign
           .bind(${h.surfaceOf(i1)}).toLazyInstance(i1)
           .bind(${h.surfaceOf(i2)}).toLazyInstance(i2)
           .bind(${h.surfaceOf(i3)}).toLazyInstance(i3)
           .bind(${h.surfaceOf(i4)}).toLazyInstance(i4)
           .bind(${h.surfaceOf(i5)}).toLazyInstance(i5)
         )
         ${h.createNewInstanceOf(a)}(session)
        }
      """
  }

  def sourceCode(c: sm.Context): c.Tree = {
    import c.universe._
    c.internal.enclosingOwner
    val pos = c.enclosingPosition
    q"wvlet.airframe.SourceCode(${""}, ${pos.source.file.name}, ${pos.line}, ${pos.column})"
  }
}
