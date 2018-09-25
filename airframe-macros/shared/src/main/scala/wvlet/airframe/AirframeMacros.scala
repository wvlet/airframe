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

  private[airframe] class BindHelper[C <: Context](val c: C) {

    import c.universe._

    def shouldGenerateTrait(t: c.Type): Boolean = {
      val a = t.typeSymbol

      // Find the public default constructor that has no arguments
      val hasPublicDefaultConstructor = t.members
        .find(_.isConstructor)
        .map(_.asMethod).exists { m =>
          m.isPublic && m.paramLists.size == 1 && m.paramLists(0).size == 0
        }

      val hasAbstractMethods = t.members.exists(x => x.isMethod && x.isAbstract && !x.isAbstractOverride)

      val isTaggedType = t.typeSymbol.fullName.startsWith("wvlet.surface.tag.")

      val shouldInstantiateTrait = if (!a.isStatic) {
        // = Non static type
        // If X is non static type (= local class or trait),
        // we need to instantiate it first in order to populate its $outer variables

        // We cannot instantiate path-dependent types
        if (t.toString.contains("#")) {
          false
        } else {
          true
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

      // Tagged type binding should be found in Design
      !isTaggedType && shouldInstantiateTrait
    }

    def bind(session: c.Tree, t: c.Type): c.Tree = {
      q"""{
            val session = ${session}
            ${newBinder(t)}(session)
          }"""
    }

    def bindSingleton(session: c.Tree, t: c.Type): c.Tree = {
      q"""{
            val session = ${session}
            ${newSingletonBinder(t)}(session)
          }"""
    }

    def findSession: c.Tree = {
      q"""wvlet.airframe.Session.findSession(this)"""
    }

    def newBinder(t: c.Type): c.Tree = {
      if (shouldGenerateTrait(t)) {
        q"""{
             session : wvlet.airframe.Session =>
             session.getOrElseUpdate(${surfaceOf(t)},
              (new $t with wvlet.airframe.SessionHolder { def airframeSession = session}).asInstanceOf[$t]
             )
            }"""
      } else {
        q"""{ session : wvlet.airframe.Session => session.get[$t](${surfaceOf(t)}) }"""
      }
    }

    def newSingletonBinder(t: c.Type): c.Tree = {
      if (shouldGenerateTrait(t)) {
        q"""{
             session : wvlet.airframe.Session =>
             session.getOrElseUpdateSingleton[$t](${surfaceOf(t)},
              (new $t with wvlet.airframe.SessionHolder { def airframeSession = session}).asInstanceOf[$t]
             )
            }"""
      } else {
        q"""{ session : wvlet.airframe.Session => session.getSingleton[$t](${surfaceOf(t)}) }"""
      }
    }

    def newFactoryBinder(i1: c.Type, a: c.Type): c.Tree = {
      if (shouldGenerateTrait(a)) {
        q"""{
             session : wvlet.airframe.Session =>
             session.getOrElseUpdateFactory[$i1, $a](${surfaceOf(i1)}, ${surfaceOf(a)},
              (new $a with wvlet.airframe.SessionHolder { def airframeSession = session}).asInstanceOf[$a]
             )
            }"""
      } else {
        q"""{ session : wvlet.airframe.Session => session.getFactory[$i1, $a](${surfaceOf(i1)}, ${surfaceOf(a)}) }"""
      }
    }

    def registorFactory(t: c.Type): c.Tree = {
      if (shouldGenerateTrait(t)) {
        q""" {
           val s = ${surfaceOf(t)}
           wvlet.airframe.factoryCache.getOrElseUpdate(s,
             { session: wvlet.airframe.Session => (new $t with wvlet.airframe.SessionHolder { def airframeSession = session }).asInstanceOf[Any] }
           )
           s
         }
        """
      } else {
        q"""{${surfaceOf(t)}}"""
      }
    }

    def withFactoryRegistration(t: c.Type, body: c.Tree): c.Tree = {
      q"""
        ${registorFactory(t)}
        ${body}
        """
    }

    def surfaceOf(t: c.Type): c.Tree = {
      q"wvlet.surface.of[$t]"
    }

    def provider1Binding[D1: c.WeakTypeTag](factory: c.Tree, singleton: Boolean, eager: Boolean): c.Tree = {
      val ev1 = implicitly[c.WeakTypeTag[D1]].tpe
      q"""{
           val self = ${c.prefix.tree}
           val d1 = ${registorFactory(ev1)}
           import wvlet.airframe.Binder._
           self.design.addBinding(ProviderBinding(DependencyFactory(self.from, Seq(d1), ${factory}), ${singleton}, ${eager}))
        }
        """
    }

    def provider2Binding[D1: c.WeakTypeTag, D2: c.WeakTypeTag](factory: c.Tree,
                                                               singleton: Boolean,
                                                               eager: Boolean): c.Tree = {
      val ev1 = implicitly[c.WeakTypeTag[D1]].tpe
      val ev2 = implicitly[c.WeakTypeTag[D2]].tpe
      q"""{
           val self = ${c.prefix.tree}
           val d1 = ${registorFactory(ev1)}
           val d2 = ${registorFactory(ev2)}
           import wvlet.airframe.Binder._
           self.design.addBinding(ProviderBinding(DependencyFactory(self.from, Seq(d1, d2), ${factory}), ${singleton}, ${eager}))
        }
        """
    }

    def provider3Binding[D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag](factory: c.Tree,
                                                                                  singleton: Boolean,
                                                                                  eager: Boolean): c.Tree = {
      val ev1 = implicitly[c.WeakTypeTag[D1]].tpe
      val ev2 = implicitly[c.WeakTypeTag[D2]].tpe
      val ev3 = implicitly[c.WeakTypeTag[D3]].tpe
      q"""{
           val self = ${c.prefix.tree}
           val d1 = ${registorFactory(ev1)}
           val d2 = ${registorFactory(ev2)}
           val d3 = ${registorFactory(ev3)}
           import wvlet.airframe.Binder._
           self.design.addBinding(ProviderBinding(DependencyFactory(self.from, Seq(d1, d2, d3), ${factory}), ${singleton}, ${eager}))
        }
        """
    }

    def provider4Binding[D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag](
        factory: c.Tree,
        singleton: Boolean,
        eager: Boolean): c.Tree = {
      val ev1 = implicitly[c.WeakTypeTag[D1]].tpe
      val ev2 = implicitly[c.WeakTypeTag[D2]].tpe
      val ev3 = implicitly[c.WeakTypeTag[D3]].tpe
      val ev4 = implicitly[c.WeakTypeTag[D4]].tpe
      q"""{
           val self = ${c.prefix.tree}
           val d1 = ${registorFactory(ev1)}
           val d2 = ${registorFactory(ev2)}
           val d3 = ${registorFactory(ev3)}
           val d4 = ${registorFactory(ev4)}
           import wvlet.airframe.Binder._
           self.design.addBinding(ProviderBinding(DependencyFactory(self.from, Seq(d1, d2, d3, d4), ${factory}), ${singleton}, ${eager}))
        }
        """
    }

    def provider5Binding[D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag, D5: c.WeakTypeTag](
        factory: c.Tree,
        singleton: Boolean,
        eager: Boolean): c.Tree = {
      val ev1 = implicitly[c.WeakTypeTag[D1]].tpe
      val ev2 = implicitly[c.WeakTypeTag[D2]].tpe
      val ev3 = implicitly[c.WeakTypeTag[D3]].tpe
      val ev4 = implicitly[c.WeakTypeTag[D4]].tpe
      val ev5 = implicitly[c.WeakTypeTag[D5]].tpe
      q"""{
           val self = ${c.prefix.tree}
           val d1 = ${registorFactory(ev1)}
           val d2 = ${registorFactory(ev2)}
           val d3 = ${registorFactory(ev3)}
           val d4 = ${registorFactory(ev4)}
           val d5 = ${registorFactory(ev5)}
           import wvlet.airframe.Binder._
           self.design.addBinding(ProviderBinding(DependencyFactory(self.from, Seq(d1, d2, d3, d4, d5), ${factory}), ${singleton}, ${eager}))
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
    h.withFactoryRegistration(t,
                              q"""${c.prefix}
         .bind(${h.surfaceOf(t)})
         .asInstanceOf[wvlet.airframe.Binder[$t]]""")
  }

  def designRemoveImpl[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    q"""{
         val d = ${c.prefix}
         val target = ${h.surfaceOf(t)}
         new wvlet.airframe.Design(d.designOptions, d.binding.filterNot(_.from == target))
        }
     """
  }

  def binderToImpl[B: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t    = implicitly[c.WeakTypeTag[B]].tpe
    val h    = new BindHelper[c.type](c)
    val core = q""" {
      val self = ${c.prefix.tree}
      val to = ${h.surfaceOf(t)}
      self.design.addBinding(wvlet.airframe.Binder.ClassBinding(self.from, to))
    }"""
    h.withFactoryRegistration(t, core)
  }

  def binderToSingletonOfImpl[B: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t    = implicitly[c.WeakTypeTag[B]].tpe
    val h    = new BindHelper[c.type](c)
    val core = q""" {
      val self = ${c.prefix.tree}
      val to = ${h.surfaceOf(t)}
      if(self.from == to) {
         wvlet.log.Logger("wvlet.airframe.Binder").warn("Binding to the same type is not allowed: " + to.toString)
         throw new wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY(Set(to))
      }
      self.design.addBinding(wvlet.airframe.Binder.SingletonBinding(self.from, to, false))
    }"""
    h.withFactoryRegistration(t, core)
  }

  def binderToEagerSingletonOfImpl[B: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t    = implicitly[c.WeakTypeTag[B]].tpe
    val h    = new BindHelper[c.type](c)
    val core = q""" {
      val self = ${c.prefix.tree}
      val to = ${h.surfaceOf(t)}
      if(self.from == to) {
         wvlet.log.Logger("wvlet.airframe.Binder").warn("Binding to the same type is not allowed: " + to.toString)
         throw new wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY(Set(to))
      }
      self.design.addBinding(wvlet.airframe.Binder.SingletonBinding(self.from, to, true))
    }"""
    h.withFactoryRegistration(t, core)
  }

  def bindToProvider1[D1: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider1Binding[D1](factory, false, false)
  }

  def bindToProvider2[D1: c.WeakTypeTag, D2: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider2Binding[D1, D2](factory, false, false)
  }

  def bindToProvider3[D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag](c: sm.Context)(
      factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider3Binding[D1, D2, D3](factory, false, false)
  }

  def bindToProvider4[D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag](c: sm.Context)(
      factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider4Binding[D1, D2, D3, D4](factory, false, false)
  }

  def bindToProvider5[D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag, D5: c.WeakTypeTag](
      c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider5Binding[D1, D2, D3, D4, D5](factory, false, false)
  }

  def bindToSingletonProvider1[D1: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider1Binding[D1](factory, true, false)
  }

  def bindToSingletonProvider2[D1: c.WeakTypeTag, D2: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider2Binding[D1, D2](factory, true, false)
  }

  def bindToSingletonProvider3[D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag](c: sm.Context)(
      factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider3Binding[D1, D2, D3](factory, true, false)
  }

  def bindToSingletonProvider4[D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag](
      c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider4Binding[D1, D2, D3, D4](factory, true, false)
  }

  def bindToSingletonProvider5[D1: c.WeakTypeTag,
                               D2: c.WeakTypeTag,
                               D3: c.WeakTypeTag,
                               D4: c.WeakTypeTag,
                               D5: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider5Binding[D1, D2, D3, D4, D5](factory, true, false)
  }

  def bindToEagerSingletonProvider1[D1: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider1Binding[D1](factory, true, true)
  }

  def bindToEagerSingletonProvider2[D1: c.WeakTypeTag, D2: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider2Binding[D1, D2](factory, true, true)
  }

  def bindToEagerSingletonProvider3[D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag](c: sm.Context)(
      factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider3Binding[D1, D2, D3](factory, true, true)
  }

  def bindToEagerSingletonProvider4[D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag](
      c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider4Binding[D1, D2, D3, D4](factory, true, true)
  }

  def bindToEagerSingletonProvider5[D1: c.WeakTypeTag,
                                    D2: c.WeakTypeTag,
                                    D3: c.WeakTypeTag,
                                    D4: c.WeakTypeTag,
                                    D5: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.provider5Binding[D1, D2, D3, D4, D5](factory, true, true)
  }

  /**
    * Used when Session location is known
    *
    * @param c
    * @tparam A
    * @return
    */
  def buildImpl[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    new BindHelper[c.type](c).bind(c.prefix.tree, t)
  }

  def buildWithSession[A: c.WeakTypeTag](c: sm.Context)(body: c.Tree): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    q"""{
           ${c.prefix}.withSession { session =>
              val a = session.build[${t}]
              ${body}(a)
           }
        }
     """
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
         session.lifeCycleManager.addInitHook(wvlet.airframe.EventHookHolder(${h.surfaceOf(t)}, dep, ${body}))
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
         session.lifeCycleManager.addInjectHook(wvlet.airframe.EventHookHolder(${h.surfaceOf(t)}, dep, ${body}))
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
         session.lifeCycleManager.addStartHook(wvlet.airframe.EventHookHolder(${h.surfaceOf(t)}, dep, ${body}))
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
         session.lifeCycleManager.addPreShutdownHook(wvlet.airframe.EventHookHolder(${h.surfaceOf(t)}, dep, ${body}))
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
         session.lifeCycleManager.addShutdownHook(wvlet.airframe.EventHookHolder(${h.surfaceOf(t)}, dep, ${body}))
         dep
        }
      """
  }

  def bindImpl[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    h.bind(h.findSession, t)
  }

  def bind0Impl[A: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdate(${h.surfaceOf(t)}, $factory)
        }
      """
  }

  def bind1Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    import c.universe._
    val h    = new BindHelper[c.type](c)
    val t    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val dep1 = h.newBinder(d1)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdate(${h.surfaceOf(t)}, $factory($dep1(session)))
        }
      """
  }

  def bind2Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    import c.universe._
    val h    = new BindHelper[c.type](c)
    val t    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdate(${h.surfaceOf(t)}, $factory($dep1(session), $dep2(session)))
        }
      """
  }

  def bind3Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag](c: sm.Context)(
      factory: c.Tree): c.Tree = {
    import c.universe._
    val h    = new BindHelper[c.type](c)
    val t    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val d3   = implicitly[c.WeakTypeTag[D3]].tpe
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    val dep3 = h.newBinder(d3)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdate(${h.surfaceOf(t)}, $factory($dep1(session),$dep2(session),$dep3(session)))
        }
      """
  }

  def bind4Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag](
      c: sm.Context)(factory: c.Tree): c.Tree = {
    import c.universe._
    val h    = new BindHelper[c.type](c)
    val t    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val d3   = implicitly[c.WeakTypeTag[D3]].tpe
    val d4   = implicitly[c.WeakTypeTag[D4]].tpe
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    val dep3 = h.newBinder(d3)
    val dep4 = h.newBinder(d4)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdate(${h.surfaceOf(t)},
           $factory($dep1(session),$dep2(session),$dep3(session),$dep4(session))
         )
        }
      """
  }

  def bind5Impl[A: c.WeakTypeTag,
                D1: c.WeakTypeTag,
                D2: c.WeakTypeTag,
                D3: c.WeakTypeTag,
                D4: c.WeakTypeTag,
                D5: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    import c.universe._
    val h    = new BindHelper[c.type](c)
    val t    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val d3   = implicitly[c.WeakTypeTag[D3]].tpe
    val d4   = implicitly[c.WeakTypeTag[D4]].tpe
    val d5   = implicitly[c.WeakTypeTag[D5]].tpe
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    val dep3 = h.newBinder(d3)
    val dep4 = h.newBinder(d4)
    val dep5 = h.newBinder(d5)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdate(${h.surfaceOf(t)},
           $factory($dep1(session),$dep2(session),$dep3(session),$dep4(session),$dep5(session))
         )
        }
      """
  }

  def bindFactoryImpl[F: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import scala.language.higherKinds
    import c.universe._
    val t  = implicitly[c.WeakTypeTag[F]].tpe // F = Function[I1, A]
    val i1 = t.typeArgs(0) // I1
    val a  = t.typeArgs(1) // A
    val h  = new BindHelper[c.type](c)

    q"""{
         val session = ${h.findSession}
         val factory: ${i1} => ${a} =
           session.getOrElseUpdateFactory(
             ${h.surfaceOf(i1)},
             ${h.surfaceOf(a)},
             ${h.newSingletonBinder(a)}(session)
           )
         factory
        }
      """
  }

  def bindSingletonImpl[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    val h = new BindHelper[c.type](c)
    val t = implicitly[c.WeakTypeTag[A]].tpe
    h.bindSingleton(h.findSession, t)
  }

  def bind0SingletonImpl[A: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    val h = new BindHelper[c.type](c)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdateSingleton(${h.surfaceOf(t)}, $factory)
        }
      """
  }

  def bind1SingletonImpl[A: c.WeakTypeTag, D1: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    import c.universe._
    val h    = new BindHelper[c.type](c)
    val t    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val dep1 = h.newBinder(d1)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdateSingleton(${h.surfaceOf(t)}, $factory($dep1(session)))
        }
      """
  }

  def bind2SingletonImpl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag](c: sm.Context)(
      factory: c.Tree): c.Tree = {
    import c.universe._
    val h    = new BindHelper[c.type](c)
    val t    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdateSingleton(${h.surfaceOf(t)}, $factory($dep1(session), $dep2(session)))
        }
      """
  }

  def bind3SingletonImpl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag](c: sm.Context)(
      factory: c.Tree): c.Tree = {
    import c.universe._
    val h    = new BindHelper[c.type](c)
    val t    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val d3   = implicitly[c.WeakTypeTag[D3]].tpe
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    val dep3 = h.newBinder(d3)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdateSingleton(${h.surfaceOf(t)}, $factory($dep1(session),$dep2(session),$dep3(session)))
        }
      """
  }

  def bind4SingletonImpl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag](
      c: sm.Context)(factory: c.Tree): c.Tree = {
    import c.universe._
    val h    = new BindHelper[c.type](c)
    val t    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val d3   = implicitly[c.WeakTypeTag[D3]].tpe
    val d4   = implicitly[c.WeakTypeTag[D4]].tpe
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    val dep3 = h.newBinder(d3)
    val dep4 = h.newBinder(d4)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdateSingleton(${h.surfaceOf(t)},
           $factory($dep1(session),$dep2(session),$dep3(session),$dep4(session))
         )
        }
      """
  }

  def bind5SingletonImpl[A: c.WeakTypeTag,
                         D1: c.WeakTypeTag,
                         D2: c.WeakTypeTag,
                         D3: c.WeakTypeTag,
                         D4: c.WeakTypeTag,
                         D5: c.WeakTypeTag](c: sm.Context)(factory: c.Tree): c.Tree = {
    import c.universe._
    val h    = new BindHelper[c.type](c)
    val t    = implicitly[c.WeakTypeTag[A]].tpe
    val d1   = implicitly[c.WeakTypeTag[D1]].tpe
    val d2   = implicitly[c.WeakTypeTag[D2]].tpe
    val d3   = implicitly[c.WeakTypeTag[D3]].tpe
    val d4   = implicitly[c.WeakTypeTag[D4]].tpe
    val d5   = implicitly[c.WeakTypeTag[D5]].tpe
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    val dep3 = h.newBinder(d3)
    val dep4 = h.newBinder(d4)
    val dep5 = h.newBinder(d5)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdateSingleton(${h.surfaceOf(t)},
           $factory($dep1(session),$dep2(session),$dep3(session),$dep4(session),$dep5(session))
         )
        }
      """
  }

}
