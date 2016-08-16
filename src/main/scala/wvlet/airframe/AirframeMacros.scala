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
import scala.reflect.{macros => sm}
import scala.language.experimental.macros
/**
  *
  */
object AirframeMacros extends LogSupport {

  def buildImpl[A: c.WeakTypeTag](c: sm.Context)(ev: c.Tree): c.Expr[A] = {
    import c.universe._
    val t = ev.tpe.typeArgs(0)
    c.Expr(
      q"""${c.prefix}.register[$t]((new $t { protected def __current_session = ${c.prefix} }).asInstanceOf[$t])"""
    )
  }

  def buildFromDesignImpl[A: c.WeakTypeTag](c: sm.Context)(ev: c.Tree): c.Expr[A] = {
    import c.universe._
    val t = ev.tpe.typeArgs(0)
    c.Expr(
      q"""{
          val session = ${c.prefix}.newSession
          session.build[$t]
        }"""
    )
  }


  def bindImpl[A: c.WeakTypeTag](c: sm.Context)(ev: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(
      q"""{
         val c = wvlet.airframe.Session.findSession(this)
         c.get(${ev})
        }
      """
    )
  }

  def bind1Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag](c: sm.Context)(factory: c.Tree)(a: c.Tree, d1: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(
      q"""{
         val c = wvlet.airframe.Session.findSession(this)
         c.getOrElseUpdate($factory(c.get(${d1})))
        }
      """
    )
  }

  def bind2Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(q"""{ val c = wvlet.airframe.Session.findSession(this); c.getOrElseUpdate($factory(c.get(${d1}), c.get(${d2}))) }""")
  }

  def bind3Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree, d3: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(q"""{ val c = wvlet.airframe.Session.findSession(this); c.getOrElseUpdate($factory(c.get(${d1}), c.get(${d2}), c.get(${d3}))) }""")
  }

  def bind4Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree, d3: c.Tree, d4: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(
      q"""{ val c = wvlet.airframe.Session.findSession(this); c.getOrElseUpdate($factory(c.get(${d1}), c.get(${d2}), c.get(${d3}), c.get(${d4}))) }""")
  }

  def bind5Impl[A: c.WeakTypeTag, D1: c.WeakTypeTag, D2: c.WeakTypeTag, D3: c.WeakTypeTag, D4: c.WeakTypeTag, D5: c.WeakTypeTag]
  (c: sm.Context)(factory: c.Tree)
  (a: c.Tree, d1: c.Tree, d2: c.Tree, d3: c.Tree, d4: c.Tree, d5: c.Tree): c.Expr[A] = {
    import c.universe._
    c.Expr(
      q"""{ val c = wvlet.airframe.Session.findSession(this); c.getOrElseUpdate($factory(c.get(${d1}), c.get(${d2}), c.get(${d3}), c.get(${d4}), c.get(${d5}))) }""")
  }

}
