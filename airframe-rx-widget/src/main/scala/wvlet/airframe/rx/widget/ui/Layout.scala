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
import wvlet.airframe.rx.widget.{RxComponent, RxComponentBuilder, RxElement}

import scala.xml.Node

/**
  *
  */
object Layout {
  def of                          = div
  def div: RxComponentBuilder     = RxComponentBuilder(tag = "div")
  def divOf(primaryClass: String) = RxComponentBuilder(tag = "div", primaryClass = primaryClass)

  def h1: RxComponentBuilder = RxComponentBuilder(tag = "h1")
  def h2: RxComponentBuilder = RxComponentBuilder(tag = "h2")
  def h3: RxComponentBuilder = RxComponentBuilder(tag = "h3")
  def h4: RxComponentBuilder = RxComponentBuilder(tag = "h4")
  def h5: RxComponentBuilder = RxComponentBuilder(tag = "h5")

  def p: RxComponentBuilder = RxComponentBuilder(tag = "p")

  def code(codeStr: String): RxElement = new RxElement {
    override def render: Node = <code>{codeStr}</code>
  }

  def codeBlock: RxComponent = new RxComponent {
    override def render(content: Node): Node =
      <pre><code>{content}</code></pre>
  }

  def scalaCode: RxComponent = new RxComponent {
    override def render(content: Node): Node = <pre><code class="language-scala rounded">{content}</code></pre>
  }
}
