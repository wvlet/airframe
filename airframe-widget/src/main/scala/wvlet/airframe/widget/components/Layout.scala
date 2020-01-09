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
package wvlet.airframe.widget.components
import wvlet.airframe.widget.{RxComponent, RxComponentBuilder, RxElement}

import scala.xml.Node

/**
  *
  */
object Layout {
  def h1: RxComponentBuilder = RxComponentBuilder(tag = "h1")
  def h2: RxComponentBuilder = RxComponentBuilder(tag = "h2")
  def h3: RxComponentBuilder = RxComponentBuilder(tag = "h3")
  def h4: RxComponentBuilder = RxComponentBuilder(tag = "h4")
  def h5: RxComponentBuilder = RxComponentBuilder(tag = "h5")

  private def newAlert(style: String): RxComponentBuilder =
    RxComponentBuilder(
      tag = "div",
      primaryClass = Some("alert"),
      otherClasses = Seq(style),
      roles = Seq("alert")
    )

  def alertPrimary: RxComponentBuilder   = newAlert("alert-primary")
  def alertSecondary: RxComponentBuilder = newAlert("alert-secondary")
  def alertSuccess: RxComponentBuilder   = newAlert("alert-success")
  def alertDanger: RxComponentBuilder    = newAlert("alert-danger")
  def alertWarning: RxComponentBuilder   = newAlert("alert-warning")
  def alertInfo                          = newAlert("alert-info")
  def alertLight                         = newAlert("alert-light")
  def alertDark                          = newAlert("alert-dark")

  def row: RxComponentBuilder = RxComponentBuilder(tag = "div", primaryClass = Some("row"))

  def colSm: RxComponent = new RxComponent {
    override def body(content: Node*): Node =
      <div class="col-sm">{content}</div>
  }

  def figure: RxComponent = new RxComponent {
    override def body(content: Node*): Node =
      <figure class="figure">{content}</figure>
  }

  def code(codeStr: String): RxElement = new RxElement {
    override def body: Node = <code>{codeStr}</code>
  }

  def codeBlock: RxComponent = new RxComponent {
    override def body(content: Node*): Node =
      <pre><code>{content}</code></pre>
  }

  def scalaCode: RxComponent = new RxComponent {
    override def body(content: Node*): Node = <pre><code class="language-scala">{content}</code></pre>
  }
}
