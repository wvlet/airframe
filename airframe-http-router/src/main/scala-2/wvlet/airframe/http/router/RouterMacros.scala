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
package wvlet.airframe.http.router

import wvlet.airframe.http.HttpFilterType

import scala.language.experimental.macros
import scala.reflect.macros.{blackbox => sm}

/**
  * Macros for creating a trait factory (Session => A) so that we can register the factory upon defining Route.of[A].
  *
  * So once you register a route for A, you don't need to call bind[A].toSingleton, etc.
  */
object RouterMacros {
  def of[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe

    if (t <:< c.typeTag[HttpFilterType].tpe) {
      q"""{
           wvlet.airframe.registerTraitFactory[${t}]
           new wvlet.airframe.http.Router(filterSurface = Some(wvlet.airframe.surface.Surface.of[${t}]))
          }
       """
    } else {
      q"""
       {
         wvlet.airframe.registerTraitFactory[${t}]
         wvlet.airframe.http.Router.empty.add[${t}]
       }
       """
    }
  }

  def add[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe

    q"""
       {
         val r = ${c.prefix}
         wvlet.airframe.registerTraitFactory[${t}]
         r.addInternal(wvlet.airframe.surface.Surface.of[${t}], wvlet.airframe.surface.Surface.methodsOf[${t}])
       }
     """
  }

  def andThen[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe

    q"""
       {
           ${c.prefix}.andThen(wvlet.airframe.http.Router.add[${t}])
       }
     """
  }
}
