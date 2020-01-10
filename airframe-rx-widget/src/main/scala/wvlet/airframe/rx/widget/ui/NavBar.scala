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
package wvlet.airframe.rx.widget.ui
import wvlet.airframe.rx.widget.RxComponent

import scala.xml.Node

/**
  *
  */
class NavBar(title: String, iconFile: String = "img/favicon.ico", iconWidth: Int = 32, iconHeight: Int = 32)
    extends RxComponent {
  override def render(content: Node*): Node =
    <nav class="navbar navbar-expand-md navbar-dark fixed-top" style="min-height: 42px; padding: 4px 8px;">
      <a class="navbar-brand" href="#">
        <img class="d-inline-block align-top" src={iconFile} alt={title} width={iconWidth.toString} hight={
      iconHeight.toString
    }/>
        {title}
      </a>
      <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarCollapse" aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
      </button>
      {content}
    </nav>
}

object NavBar {

  def fixedTop(title: String) = new NavBar(title)

}
