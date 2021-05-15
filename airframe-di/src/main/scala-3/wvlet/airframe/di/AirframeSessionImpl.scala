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

import wvlet.airframe.surface.Surface

private[di] trait AirframeSessionImpl { self: AirframeSession =>
  inline override def register[A](instance: A): Unit = ${ AirframeSessionImpl.registerImpl[A]('self, 'instance) }
}

private[di] object AirframeSessionImpl {
  import scala.quoted._

  def registerImpl[A](session: Expr[AirframeSession], instance:Expr[A])(using Type[A], Quotes): Expr[Unit] = {
    '{
        {
          val surface = Surface.of[A]
          val ss = ${session}
          val owner = ss.findOwnerSessionOf(surface).getOrElse(ss)
          owner.registerInjectee(surface, surface, ${instance})
          ()
        }
    }
  }
}
