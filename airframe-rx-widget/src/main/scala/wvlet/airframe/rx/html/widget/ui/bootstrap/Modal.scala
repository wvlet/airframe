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
package wvlet.airframe.rx.html.widget.ui.bootstrap
import wvlet.airframe.rx.html.{EntityRef, RxComponent, RxElement}
import wvlet.airframe.rx.html.all._

case class Modal(title: String, footer: RxElement = div()) extends RxComponent {

  def withFooter(footer: RxElement): Modal = this.copy(footer = footer)

  override def render(content: RxElement): RxElement =
    div(
      _class   -> "modal",
      style    -> "display: block; position: relative;",
      tabindex -> "-1",
      role     -> "dialog",
      div(
        _class -> "modal-dialog",
        role   -> "document",
        div(
          _class -> "modal-content",
          div(
            _class -> "modal-header",
            h5(_class -> "modal-title", title),
            button(
              _type           -> "button",
              _class          -> "close",
              data("dismiss") -> "modal",
              aria.label      -> "Close",
              span(aria.hidden -> "true", EntityRef("times"))
            )
          ),
          div(_class -> "modal-body", content),
          div(_class -> "modal-footer", footer)
        )
      )
    )
}

/**
  */
object Modal {

  def default(title: String) = Modal(title)
}
