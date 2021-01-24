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
package wvlet.airframe.rx.html.widget.auth

/**
  */

import org.scalajs.dom.raw.MouseEvent
import wvlet.airframe.rx.html.RxElement
import wvlet.airframe.rx.html.all._
import wvlet.log.LogSupport

/**
  */
class GoogleAuthButton extends RxElement with LogSupport {

  override def render: RxElement = {
    GoogleAuth.currentUser.transform {
      case Some(u) =>
        div(
          cls -> "dropdown",
          span(
            cls -> "d-flex flex-nowrap align-middle ml-2",
            img(
              cls    -> "rounded",
              width  -> "32px",
              height -> "32px",
              title  -> s"${u.name} ${u.email}",
              src    -> u.imageUrl
            ),
            a(
              cls            -> "btn btn-secondary btn-sm dropdown-toggle",
              href           -> "#",
              role           -> "button",
              id             -> "loginLink",
              data("toggle") -> "dropdown",
              aria.haspopup  -> true,
              aria.expanded  -> false
            ),
            div(
              cls             -> "dropdown-menu dropdown-menu-right",
              aria.labelledby -> "loginLink",
              h6(cls  -> "dropdown-header", s"${u.name}: ${u.email}"),
              div(cls -> "dropdown-divider"),
              a(
                cls  -> "dropdown-item",
                href -> "#",
                "Sign out",
                onclick -> { e: MouseEvent =>
                  GoogleAuth.signOut
                }
              )
            )
          )
        )
      case None =>
        span(
          cls -> "g-signin2",
          // Hide this tag while initializing Google API
          style -> GoogleAuth.loading.map { x =>
            s"display: ${if (x) "none" else "inline"};"
          },
          data("theme") -> "dark"
        )
    }
  }
}
