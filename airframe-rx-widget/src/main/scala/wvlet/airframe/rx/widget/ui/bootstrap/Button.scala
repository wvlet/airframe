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

import org.scalajs.dom
import wvlet.airframe.rx.widget.RxElement
import wvlet.log.LogSupport

/**
  *
  */
case class Button(
    name: String,
    private var disabled: Boolean = false
) extends RxElement {

  def small: Button = addClass("btn-sm")

  def isActive: Boolean   = !disabled
  def isDisabled: Boolean = disabled

  def active: Button = {
    disabled = false
    this
  }
  def disable: Button = {
    disabled = true
    this
  }

  def render: xml.Node = {
    if (isActive) {
      <button type="button" class="btn" onclick={
        config.onClickHandler.getOrElse { x: dom.MouseEvent =>
        }
      }>{name}</button>
    } else {
      <button type="button" class="btn" disabled="true">{name}</button>
    }
  }

  def onClick(handler: dom.MouseEvent => Unit): Button = {
    updateConfig(config.onClick(handler))
    this
  }

}

object Button extends LogSupport {
  def primary(name: String)   = Button(name).addClass("btn-primary")
  def secondary(name: String) = Button(name).addClass("btn-secondary")
  def success(name: String)   = Button(name).addClass("btn-success")
  def danger(name: String)    = Button(name).addClass("btn-danger")
  def warning(name: String)   = Button(name).addClass("btn-warning")
  def info(name: String)      = Button(name).addClass("btn-info")
  def light(name: String)     = Button(name).addClass("btn-light")
  def dark(name: String)      = Button(name).addClass("btn-dark")
  def link(name: String)      = Button(name).addClass("btn-link")
}
