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
  def div(primaryClass: String) = RxComponentBuilder(tag = "div", primaryClass = primaryClass)

  def container: RxComponentBuilder      = div("container")
  def containerFluid: RxComponentBuilder = div("container-fluid")

  def h1: RxComponentBuilder = RxComponentBuilder(tag = "h1")
  def h2: RxComponentBuilder = RxComponentBuilder(tag = "h2")
  def h3: RxComponentBuilder = RxComponentBuilder(tag = "h3")
  def h4: RxComponentBuilder = RxComponentBuilder(tag = "h4")
  def h5: RxComponentBuilder = RxComponentBuilder(tag = "h5")

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

  def row: RxComponentBuilder   = div("row")
  def col: RxComponentBuilder   = div("col")
  def col1: RxComponentBuilder  = div("col-1")
  def col2: RxComponentBuilder  = div("col-2")
  def col3: RxComponentBuilder  = div("col-3")
  def col4: RxComponentBuilder  = div("col-4")
  def col5: RxComponentBuilder  = div("col-5")
  def col6: RxComponentBuilder  = div("col-6")
  def col7: RxComponentBuilder  = div("col-7")
  def col8: RxComponentBuilder  = div("col-8")
  def col9: RxComponentBuilder  = div("col-9")
  def col10: RxComponentBuilder = div("col-10")
  def col11: RxComponentBuilder = div("col-11")
  def col12: RxComponentBuilder = div("col-12")
  def colSm: RxComponentBuilder = div("col-sm")

  def figure: RxComponentBuilder = RxComponentBuilder(tag = "figure", primaryClass = "figure")

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
