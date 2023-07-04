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
package wvlet.airframe.rx.html

import wvlet.airframe.rx.Cancelable

trait HtmlNodeBase { self: HtmlNode =>

  /**
    * (Scala.js only) Render this element to the DOM node of the given ID. If the corresponding DOM node is not found,
    * this method will create a new DOM node.
    *
    * @param nodeId
    * @return
    *   a Cancelable object to clean up the rendered elements
    */
  def renderTo(nodeId: String): Cancelable = {
    compat.renderTo(nodeId, this)
  }
}
