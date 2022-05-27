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

import wvlet.airframe.http.Router
import wvlet.airframe.http.HttpFilterType
import wvlet.airframe.surface.Surface

trait RouterBase { self: Router =>
  inline def add[Controller]: Router = {
    self.addInternal(Surface.of[Controller], Surface.methodsOf[Controller])
  }

  inline def andThen[Controller]: Router = {
    self.andThen(Router.add[Controller])
  }
}

trait RouterObjectBase {
  inline def of[Controller]: Router = ${ RouterObjectMacros.routerOf[Controller] }

  inline def add[Controller]: Router = ${ RouterObjectMacros.routerOf[Controller] }
}

private[router] object RouterObjectMacros {
  import scala.quoted._

  def routerOf[Controller: Type](using quotes: Quotes): Expr[Router] = {
    import quotes._
    import quotes.reflect._

    if (TypeRepr.of[Controller] <:< TypeRepr.of[HttpFilterType]) {
      '{ Router(filterSurface = Some(Surface.of[Controller])) }
    } else {
      '{ Router.empty.add[Controller] }
    }
  }
}
