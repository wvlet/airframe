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
package wvlet.airframe.rx.widget.ui.bootstrap

import wvlet.airframe.rx.html.{HtmlElement, HtmlNode, RxComponent}
import wvlet.airframe.rx.html.all._

/**
  *
  */
case class NavBar(title: String, iconFile: String = "img/favicon.ico", iconWidth: Int = 32, iconHeight: Int = 32)
    extends RxComponent {
  override def render(content: HtmlNode): HtmlNode =
    nav(
      cls   -> "navbar navbar-expand-md navbar-dark fixed-top",
      style -> "min-height: 42px; padding: 4px 8px;",
      a(
        cls  -> "navbar-brand",
        href -> "#",
        img(cls -> "d-inline-block align-top", src -> iconFile, alt -> title, width -> iconWidth, height -> iconHeight),
        { title }
      ),
      button(
        cls            -> "navbar-toggler",
        tpe            -> "button",
        data("toggle") -> "collapse",
        data("target") -> "#navbarCollapse",
        aria.controls  -> "navbarCollapse",
        aria.expanded  -> "false",
        aria.label     -> "Toggle navigation",
        span(cls -> "navbar-toggler-icon")
      ) { content }
    )
}

object NavBar {

  def fixedTop(title: String): RxComponent = new NavBar(title)

  def item: RxComponent = RxComponent { content =>
    li(_class -> "nav-item active", a(_class -> "nav-link", href -> "#", "Home ", span(_class -> "sr-only", "current")))
  }

  def navList: RxComponent = RxComponent { content =>
    div(_class -> "collapse navbar-collapse", id -> "navbarCollapse", ul(_class -> "navbar-nav mr-auto", content))
  }

  def navItemActive   = navItemCustom(active = true)
  def navItemDisabled = navItemCustom(disabled = true)
  def navItem         = navItemCustom()

  private def navItemCustom(active: Boolean = false, disabled: Boolean = false) = RxComponent { content =>
    val itemStyle = s"nav-item${if (active) " active" else ""}"
    val linkStyle = s"nav-link${if (disabled) " disabled" else ""}"
    li(_class -> itemStyle, a(_class -> linkStyle, href -> "#", content))
  }

  def sideBarSticky = RxComponent { content =>
    nav(
      _class -> "collapse navbar-collapse col-md-2 d-none d-md-block sidebar bg-light",
      div(_class -> "sidebar-sticky", ul(_class -> "nav flex-column", content))
    )
  }

}
