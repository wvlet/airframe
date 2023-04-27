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
package wvlet.airframe.http
import scala.language.experimental.macros
import scala.reflect.macros.{blackbox => sm}

private[http] object RxRouterMacros {

  def of[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe

    q"""
     {
       wvlet.airframe.registerTraitFactory[${t}]
       wvlet.airframe.http.RxRouter.EndpointNode(wvlet.airframe.surface.Surface.of[${t}], wvlet.airframe.surface.Surface.methodsOf[${t}])
     }
   """
  }

  def filter[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe

    if (t <:< c.typeTag[wvlet.airframe.http.RxHttpFilter].tpe) {
      q"""
       {
         wvlet.airframe.registerTraitFactory[${t}]
         wvlet.airframe.http.RxRouter.FilterNode(None, wvlet.airframe.surface.Surface.of[${t}])
       }
     """
    } else {
      c.error(c.enclosingPosition, s"${t} is not a RxFilter type")
      q""""""
    }
  }

  def andThenFilter[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe

    if (t <:< c.typeTag[wvlet.airframe.http.RxHttpFilter].tpe) {
      q"""
     {
       wvlet.airframe.registerTraitFactory[${t}]
       val next = wvlet.airframe.http.RxRouter.FilterNode(None, wvlet.airframe.surface.Surface.of[${t}])
       ${c.prefix}.andThen(next)
     }
   """
    } else {
      c.error(c.enclosingPosition, s"${t} is not a RxFilter type")
      q""""""
    }
  }
}
