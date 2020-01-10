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
  def list(elems: RxElement*) = div.apply(elems: _*)

  def div: RxComponentBuilder     = RxComponentBuilder(tag = "div")
  def divOf(primaryClass: String) = RxComponentBuilder(tag = "div", primaryClass = primaryClass)

  def container: RxComponentBuilder      = divOf("container")
  def containerFluid: RxComponentBuilder = divOf("container-fluid")

  def flexbox: RxComponentBuilder       = divOf("d-flex")
  def inlineFlexbox: RxComponentBuilder = divOf("d-inline-flex")

  def h1: RxComponentBuilder = RxComponentBuilder(tag = "h1")
  def h2: RxComponentBuilder = RxComponentBuilder(tag = "h2")
  def h3: RxComponentBuilder = RxComponentBuilder(tag = "h3")
  def h4: RxComponentBuilder = RxComponentBuilder(tag = "h4")
  def h5: RxComponentBuilder = RxComponentBuilder(tag = "h5")

  def p: RxComponentBuilder = RxComponentBuilder(tag = "p")

  private def newAlert(style: String): RxComponentBuilder = {
    RxComponentBuilder(tag = "div")
      .withClasses("alert", style)
      .withRoles("alert")
  }

  def alertPrimary: RxComponentBuilder   = newAlert("alert-primary")
  def alertSecondary: RxComponentBuilder = newAlert("alert-secondary")
  def alertSuccess: RxComponentBuilder   = newAlert("alert-success")
  def alertDanger: RxComponentBuilder    = newAlert("alert-danger")
  def alertWarning: RxComponentBuilder   = newAlert("alert-warning")
  def alertInfo                          = newAlert("alert-info")
  def alertLight                         = newAlert("alert-light")
  def alertDark                          = newAlert("alert-dark")

  def row: RxComponentBuilder   = divOf("row")
  def col: RxComponentBuilder   = divOf("col")
  def col1: RxComponentBuilder  = divOf("col-1")
  def col2: RxComponentBuilder  = divOf("col-2")
  def col3: RxComponentBuilder  = divOf("col-3")
  def col4: RxComponentBuilder  = divOf("col-4")
  def col5: RxComponentBuilder  = divOf("col-5")
  def col6: RxComponentBuilder  = divOf("col-6")
  def col7: RxComponentBuilder  = divOf("col-7")
  def col8: RxComponentBuilder  = divOf("col-8")
  def col9: RxComponentBuilder  = divOf("col-9")
  def col10: RxComponentBuilder = divOf("col-10")
  def col11: RxComponentBuilder = divOf("col-11")
  def col12: RxComponentBuilder = divOf("col-12")
  def colSm: RxComponentBuilder = divOf("col-sm")

  def figure: RxComponentBuilder = RxComponentBuilder(tag = "figure", primaryClass = "figure")

  def code(codeStr: String): RxElement = new RxElement {
    override def render: Node = <code>{codeStr}</code>
  }

  def codeBlock: RxComponent = new RxComponent {
    override def render(content: Node*): Node =
      <pre><code>{content}</code></pre>
  }

  def scalaCode: RxComponent = new RxComponent {
    override def render(content: Node*): Node = <pre><code class="language-scala rounded">{content}</code></pre>
  }
}
