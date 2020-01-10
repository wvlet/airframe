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
class SideBar extends RxComponent {
  override def render(content: Node*): Node =
    <nav class="collapse navbar-collapse col-md-2 d-none d-md-block sidebar bg-light">
      <div class="sidebar-sticky">
        {content}
      </div>
    </nav>
}

object SideBar {

  def sticky = new SideBar()
}
