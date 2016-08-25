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

import wvlet.log.LogSupport

import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context
import scala.reflect.{macros => sm}

object AirframeMacros extends LogSupport {

  class BindHelper[C <: Context](val c: C) {

    def bind(session: c.Tree, typeEv: c.Tree): c.Tree = {
      import c.universe._

      val t = typeEv.tpe.typeArgs(0)
      val a = t.typeSymbol

      // Find the pubilc default constructor that has no arguments
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
        // needs to be found in the design unless it has the default constructor
        hasPublicDefaultConstructor && !hasAbstractMethods
      }
      else {
        // We cannot instantiate any trait or class without the default constructor
        // So binding needs to be find
        hasPublicDefaultConstructor
      }

      if (!isTaggedType && shouldInstantiateTrait) {
        q"""{
          val session = ${session}
          session.getOrElseUpdate[$t]((new $t {
               protected[this] def __current_session = session
             }).asInstanceOf[$t])
          }"""
      }
      else {
        q"""{
            val session = ${session}
           session.get[$t]
           }"""
      }
    }
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

  def bindImpl[A: c.WeakTypeTag](c: sm.Context)(ev: c.Tree): c.Tree = {
    import c.universe._
    new BindHelper[c.type](c).bind(q"wvlet.airframe.Session.findSession(this)", ev)
  }

  def addLifeCycle(c: sm.Context): c.Tree = {
    import c.universe._
    q"""{
         val session = wvlet.airframe.Session.findSession(this)
         new wvlet.airframe.LifeCycleBinder(${c.prefix}.dep, session)
        }
      """
  }

  def bind0Impl[A: c.WeakTypeTag](c: sm.Context)(factory: c.Tree)(a: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(
      q"""{
         val session = wvlet.airframe.Session.findSession(this)
         session.getOrElseUpdate($factory())
        }
      """
    )
  }

  def bind1Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)(a: c.Tree, d1: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(
      q"""{
         val session = wvlet.airframe.Session.findSession(this)
         session.getOrElseUpdate($factory(session.get(${d1})))
        }
      """
    )
  }

  def bind2Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(
      q"""{
         val session = wvlet.airframe.Session.findSession(this);
         c.getOrElseUpdate($factory(session.get(${d1}), session.get(${d2})))
         }
        """)
  }

  def bind3Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree, d3: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(
      q"""{
         val session = wvlet.airframe.Session.findSession(this);
         session.getOrElseUpdate($factory(session.get(${d1}),
           session.get(${d2}), session.get(${d3})))
         }""")
  }

  def bind4Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag,
  D3: c.WeakTypeTag, D4: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree, d3: c.Tree, d4: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(
      q"""{
         val session = wvlet.airframe.Session.findSession(this);
         session.getOrElseUpdate($factory(session.get(${d1}), session.get(${d2}),
           session.get(${d3}), session.get(${d4})))
         }""")
  }

  def bind5Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag,
  D3: c.WeakTypeTag, D4: c.WeakTypeTag, D5: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree, d3: c.Tree, d4: c.Tree, d5: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(
      q"""{
         val session = wvlet.airframe.Session.findSession(this);
         session.getOrElseUpdate($factory(session.get(${d1}), session.get(${d2}),
           session.get(${d3}), session.get(${d4}), session.get(${d5})))
         }""")
  }
}
