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

      val hasAbstractMethods = t.members.exists(x =>
        x.isMethod && x.isAbstract && !x.isAbstractOverride
      )

      val isTaggedType = t.typeSymbol.fullName.startsWith("wvlet.obj.tag.")

      val shouldInstantiateTrait = if (!a.isStatic) {
        // = Non static type
        // If X is non static type (= local class or trait),
        // we need to instantiate it first in order to populate its $outer variables
        true
      }
      else if (a.isAbstract) {
        // = Abstract type
        // We cannot build abstract type X that has abstract methods, so bind[X].to[ConcreteType]
        // needs to be found in the design

        // If there is no abstract methods, it might be a trait without any method
        !hasAbstractMethods
      }
      else {
        // We cannot instantiate any trait or class without the default constructor
        // So binding needs to be found in the Design.
        hasPublicDefaultConstructor
      }

      // Tagged type binding should be found in Design
      !isTaggedType && shouldInstantiateTrait
    }

    def bind(session: c.Tree, typeEv: c.Tree): c.Tree = {
      q"""{
            val session = ${session}
            ${newBinder(typeEv)}(session)
          }"""
    }

    def bindSingleton(session: c.Tree, typeEv: c.Tree): c.Tree = {
      q"""{
            val session = ${session}
            ${newSingletonBinder(typeEv)}(session)
          }"""
    }

    def findSession: c.Tree = {
      q"wvlet.airframe.Session.findSession(this)"
    }

    def newBinder(typeEv: c.Tree): c.Tree = {
      val t = typeEv.tpe.typeArgs(0)
      if (shouldGenerateTrait(t)) {
        q"""{
             session : wvlet.airframe.Session =>
             session.getOrElseUpdate[$t](
              (new $t { protected[this] def __current_session = session}).asInstanceOf[$t]
             )
            }"""
      }
      else {
        q"""{ session : wvlet.airframe.Session => session.get[$t] }"""
      }
    }

    def newSingletonBinder(typeEv: c.Tree): c.Tree = {
      val t = typeEv.tpe.typeArgs(0)
      if (shouldGenerateTrait(t)) {
        q"""{
             session : wvlet.airframe.Session =>
             session.getOrElseUpdateSingleton[$t](
              (new $t { protected[this] def __current_session = session}).asInstanceOf[$t]
             )
            }"""
      }
      else {
        q"""{ session : wvlet.airframe.Session => session.getSingleton[$t] }"""
      }
    }

    def registorFactory(typeEv:c.Tree) : c.Tree = {
      val t = typeEv.tpe.typeArgs(0)
      if(shouldGenerateTrait(t)) {
        q""" {
           wvlet.airframe.factoryCache.getOrElseUpdate(classOf[$t],
             { session: wvlet.airframe.Session => (new $t { protected def __current_session = session }).asInstanceOf[Any] }
           )
         }
        """
      }
      else {
        q"""{}"""
      }
    }

    def withFactoryRegistration(typeEv: c.Tree, body: c.Tree) : c.Tree = {
      val t = typeEv.tpe.typeArgs(0)
      q"""
        ${registorFactory(typeEv)}
        ${body}
        """
    }
  }

  def designBindImpl[A: c.WeakTypeTag](c: sm.Context)(ev:c.Tree): c.Tree = {
    import c.universe._
    val t = ev.tpe.typeArgs(0)
    new BindHelper[c.type](c).withFactoryRegistration(ev, q"${c.prefix}.bind(wvlet.obj.ObjectType.of[$t]).asInstanceOf[wvlet.airframe.Binder[$t]]")
  }

  def binderToImpl[B: c.WeakTypeTag](c: sm.Context)(ev: c.Tree) : c.Tree = {
    import c.universe._

    val t = ev.tpe.typeArgs(0)
    val core = q""" {
      val self = ${c.prefix.tree}
      val to = wvlet.obj.ObjectType.of[$t]
      self.design.addBinding(wvlet.airframe.Binder.ClassBinding(self.from, to))
    }"""
    new BindHelper[c.type](c).withFactoryRegistration(ev, core)
  }

  def binderToSingletonOfImpl[B: c.WeakTypeTag](c: sm.Context)(ev: c.Tree) : c.Tree = {
    import c.universe._

    val t = ev.tpe.typeArgs(0)
    val core = q""" {
      val self = ${c.prefix.tree}
      val to = wvlet.obj.ObjectType.of[$t]
      if(self.from == to) {
         wvlet.log.Logger("wvlet.airframe.Binder").warn("Binding to the same type is not allowed: $${to}")
         throw new wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY(Set(to))
      }
      self.design.addBinding(wvlet.airframe.Binder.SingletonBinding(self.from, to, false))
    }"""
    new BindHelper[c.type](c).withFactoryRegistration(ev, core)
  }

  def binderToEagerSingletonOfImpl[B: c.WeakTypeTag](c: sm.Context)(ev: c.Tree) : c.Tree = {
    import c.universe._

    val t = ev.tpe.typeArgs(0)
    val core = q""" {
      val self = ${c.prefix.tree}
      val to = wvlet.obj.ObjectType.of[$t]
      if(self.from == to) {
         wvlet.log.Logger("wvlet.airframe.Binder").warn("Binding to the same type is not allowed: $${to}")
         throw new wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY(Set(to))
      }
      self.design.addBinding(wvlet.airframe.Binder.SingletonBinding(self.from, to, true))
    }"""
    new BindHelper[c.type](c).withFactoryRegistration(ev, core)
  }

  def bindToProvider1[D1:c.WeakTypeTag](c:sm.Context)(factory:c.Tree)(ev1:c.Tree) : c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
           val self = ${c.prefix.tree}
           ${h.registorFactory(ev1)}
           self.toProviderD1(${factory}, false, false)
        }
    """
  }

  def bindToProvider2[D1:c.WeakTypeTag, D2:c.WeakTypeTag](c:sm.Context)(factory:c.Tree)(ev1:c.Tree, ev2:c.Tree) : c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
           val self = ${c.prefix.tree}
           ${h.registorFactory(ev1)}
           ${h.registorFactory(ev2)}
           self.toProviderD2(${factory}, false, false)
        }
    """
  }

  def bindToProvider3[D1:c.WeakTypeTag, D2:c.WeakTypeTag, D3:c.WeakTypeTag](c:sm.Context)(factory:c.Tree)(ev1:c.Tree, ev2:c.Tree, ev3:c.Tree) : c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
           val self = ${c.prefix.tree}
           ${h.registorFactory(ev1)}
           ${h.registorFactory(ev2)}
           ${h.registorFactory(ev3)}
           self.toProviderD3(${factory}, false, false)
        }
    """
  }

  def bindToProvider4[D1:c.WeakTypeTag, D2:c.WeakTypeTag, D3:c.WeakTypeTag, D4:c.WeakTypeTag](c:sm.Context)(factory:c.Tree)(ev1:c.Tree, ev2:c.Tree, ev3:c.Tree, ev4:c.Tree) : c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
           val self = ${c.prefix.tree}
           ${h.registorFactory(ev1)}
           ${h.registorFactory(ev2)}
           ${h.registorFactory(ev3)}
           ${h.registorFactory(ev4)}
           self.toProviderD4(${factory}, false, false)
        }
    """
  }

  def bindToProvider5[D1:c.WeakTypeTag, D2:c.WeakTypeTag, D3:c.WeakTypeTag, D4:c.WeakTypeTag, D5:c.WeakTypeTag](c:sm.Context)(factory:c.Tree)(ev1:c.Tree, ev2:c.Tree, ev3:c.Tree, ev4:c.Tree, ev5:c.Tree) : c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
           val self = ${c.prefix.tree}
           ${h.registorFactory(ev1)}
           ${h.registorFactory(ev2)}
           ${h.registorFactory(ev3)}
           ${h.registorFactory(ev4)}
           ${h.registorFactory(ev5)}
           self.toProviderD5(${factory}, false, false)
        }
    """
  }

  def bindToSingletonProvider1[D1:c.WeakTypeTag](c:sm.Context)(factory:c.Tree)(ev1:c.Tree) : c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
           val self = ${c.prefix.tree}
           ${h.registorFactory(ev1)}
           self.toProviderD1(${factory}, true, false)
        }
    """
  }

  def bindToSingletonProvider2[D1:c.WeakTypeTag, D2:c.WeakTypeTag](c:sm.Context)(factory:c.Tree)(ev1:c.Tree, ev2:c.Tree) : c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
           val self = ${c.prefix.tree}
           ${h.registorFactory(ev1)}
           ${h.registorFactory(ev2)}
           self.toProviderD2(${factory}, true, false)
        }
    """
  }

  def bindToSingletonProvider3[D1:c.WeakTypeTag, D2:c.WeakTypeTag, D3:c.WeakTypeTag](c:sm.Context)(factory:c.Tree)(ev1:c.Tree, ev2:c.Tree, ev3:c.Tree) : c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
           val self = ${c.prefix.tree}
           ${h.registorFactory(ev1)}
           ${h.registorFactory(ev2)}
           ${h.registorFactory(ev3)}
           self.toProviderD3(${factory}, true, false)
        }
    """
  }

  def bindToSingletonProvider4[D1:c.WeakTypeTag, D2:c.WeakTypeTag, D3:c.WeakTypeTag, D4:c.WeakTypeTag](c:sm.Context)(factory:c.Tree)(ev1:c.Tree, ev2:c.Tree, ev3:c.Tree, ev4:c.Tree) : c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
           val self = ${c.prefix.tree}
           ${h.registorFactory(ev1)}
           ${h.registorFactory(ev2)}
           ${h.registorFactory(ev3)}
           ${h.registorFactory(ev4)}
           self.toProviderD4(${factory}, true, false)
        }
    """
  }

  def bindToSingletonProvider5[D1:c.WeakTypeTag, D2:c.WeakTypeTag, D3:c.WeakTypeTag, D4:c.WeakTypeTag, D5:c.WeakTypeTag](c:sm.Context)(factory:c.Tree)(ev1:c.Tree, ev2:c.Tree, ev3:c.Tree, ev4:c.Tree, ev5:c.Tree) : c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
           val self = ${c.prefix.tree}
           ${h.registorFactory(ev1)}
           ${h.registorFactory(ev2)}
           ${h.registorFactory(ev3)}
           ${h.registorFactory(ev4)}
           ${h.registorFactory(ev5)}
           self.toProviderD5(${factory}, true, false)
        }
    """
  }

  def bindToEagerSingletonProvider1[D1:c.WeakTypeTag](c:sm.Context)(factory:c.Tree)(ev1:c.Tree) : c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
           val self = ${c.prefix.tree}
           ${h.registorFactory(ev1)}
           self.toProviderD1(${factory}, true, true)
        }
    """
  }

  def bindToEagerSingletonProvider2[D1:c.WeakTypeTag, D2:c.WeakTypeTag](c:sm.Context)(factory:c.Tree)(ev1:c.Tree, ev2:c.Tree) : c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
           val self = ${c.prefix.tree}
           ${h.registorFactory(ev1)}
           ${h.registorFactory(ev2)}
           self.toProviderD2(${factory}, true, true)
        }
    """
  }

  def bindToEagerSingletonProvider3[D1:c.WeakTypeTag, D2:c.WeakTypeTag, D3:c.WeakTypeTag](c:sm.Context)(factory:c.Tree)(ev1:c.Tree, ev2:c.Tree, ev3:c.Tree) : c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
           val self = ${c.prefix.tree}
           ${h.registorFactory(ev1)}
           ${h.registorFactory(ev2)}
           ${h.registorFactory(ev3)}
           self.toProviderD3(${factory}, true, true)
        }
    """
  }

  def bindToEagerSingletonProvider4[D1:c.WeakTypeTag, D2:c.WeakTypeTag, D3:c.WeakTypeTag, D4:c.WeakTypeTag](c:sm.Context)(factory:c.Tree)(ev1:c.Tree, ev2:c.Tree, ev3:c.Tree, ev4:c.Tree) : c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
           val self = ${c.prefix.tree}
           ${h.registorFactory(ev1)}
           ${h.registorFactory(ev2)}
           ${h.registorFactory(ev3)}
           ${h.registorFactory(ev4)}
           self.toProviderD4(${factory}, true, true)
        }
    """
  }

  def bindToEagerSingletonProvider5[D1:c.WeakTypeTag, D2:c.WeakTypeTag, D3:c.WeakTypeTag, D4:c.WeakTypeTag, D5:c.WeakTypeTag](c:sm.Context)(factory:c.Tree)(ev1:c.Tree, ev2:c.Tree, ev3:c.Tree, ev4:c.Tree, ev5:c.Tree) : c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
           val self = ${c.prefix.tree}
           ${h.registorFactory(ev1)}
           ${h.registorFactory(ev2)}
           ${h.registorFactory(ev3)}
           ${h.registorFactory(ev4)}
           ${h.registorFactory(ev5)}
           self.toProviderD5(${factory}, true, true)
        }
    """
  }

  /**
    * Used when Session location is known
    *
    * @param c
    * @param ev
    * @tparam A
    * @return
    */
  def buildImpl[A: c.WeakTypeTag](c: sm.Context)(ev: c.Tree): c.Tree = {
    new BindHelper[c.type](c).bind(c.prefix.tree, ev)
  }

  def addLifeCycle(c: sm.Context): c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
         val session = ${h.findSession}
         new wvlet.airframe.LifeCycleBinder(${c.prefix}.dep, session)
        }
      """
  }

  def bindImpl[A: c.WeakTypeTag](c: sm.Context)(ev: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.bind(h.findSession, ev)
  }

  def bind0Impl[A: c.WeakTypeTag](c: sm.Context)(factory: c.Tree)(a: c.Tree): c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdate($factory)
        }
      """
  }

  def bind1Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)(a: c.Tree, d1: c.Tree): c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    val dep1 = h.newBinder(d1)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdate($factory($dep1(session)))
        }
      """
  }

  def bind2Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree): c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdate($factory($dep1(session), $dep2(session)))
        }
      """
  }

  def bind3Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree, d3: c.Tree): c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    val dep3 = h.newBinder(d3)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdate($factory($dep1(session),$dep2(session),$dep3(session)))
        }
      """
  }

  def bind4Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag,
  D3: c.WeakTypeTag, D4: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree, d3: c.Tree, d4: c.Tree): c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    val dep3 = h.newBinder(d3)
    val dep4 = h.newBinder(d4)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdate(
           $factory($dep1(session),$dep2(session),$dep3(session),$dep4(session))
         )
        }
      """
  }

  def bind5Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag,
  D3: c.WeakTypeTag, D4: c.WeakTypeTag, D5: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree, d3: c.Tree, d4: c.Tree, d5: c.Tree): c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    val dep3 = h.newBinder(d3)
    val dep4 = h.newBinder(d4)
    val dep5 = h.newBinder(d5)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdate(
           $factory($dep1(session),$dep2(session),$dep3(session),$dep4(session),$dep5(session))
         )
        }
      """
  }

  def bindSingletonImpl[A: c.WeakTypeTag](c: sm.Context)(ev: c.Tree): c.Tree = {
    val h = new BindHelper[c.type](c)
    h.bindSingleton(h.findSession, ev)
  }

  def bind0SingletonImpl[A: c.WeakTypeTag](c: sm.Context)(factory: c.Tree)(a: c.Tree): c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdateSingleton($factory)
        }
      """
  }

  def bind1SingletonImpl[A: c.WeakTypeTag, D1: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)(a: c.Tree, d1: c.Tree): c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    val dep1 = h.newBinder(d1)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdateSingleton($factory($dep1(session)))
        }
      """
  }

  def bind2SingletonImpl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree): c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdateSingleton($factory($dep1(session), $dep2(session)))
        }
      """
  }

  def bind3SingletonImpl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree, d3: c.Tree): c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    val dep3 = h.newBinder(d3)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdateSingleton($factory($dep1(session),$dep2(session),$dep3(session)))
        }
      """
  }

  def bind4SingletonImpl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag,
  D3: c.WeakTypeTag, D4: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree, d3: c.Tree, d4: c.Tree): c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    val dep3 = h.newBinder(d3)
    val dep4 = h.newBinder(d4)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdateSingleton(
           $factory($dep1(session),$dep2(session),$dep3(session),$dep4(session))
         )
        }
      """
  }

  def bind5SingletonImpl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag,
  D3: c.WeakTypeTag, D4: c.WeakTypeTag, D5: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree, d3: c.Tree, d4: c.Tree, d5: c.Tree): c.Tree = {
    import c.universe._
    val h = new BindHelper[c.type](c)
    val dep1 = h.newBinder(d1)
    val dep2 = h.newBinder(d2)
    val dep3 = h.newBinder(d3)
    val dep4 = h.newBinder(d4)
    val dep5 = h.newBinder(d5)
    q"""{
         val session = ${h.findSession}
         session.getOrElseUpdateSingleton(
           $factory($dep1(session),$dep2(session),$dep3(session),$dep4(session),$dep5(session))
         )
        }
      """
  }

}
