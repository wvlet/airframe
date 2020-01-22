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

import wvlet.airframe.rx.html._
import wvlet.airframe.rx.html.all._
import wvlet.log.LogSupport

/**
  *
  */
case class Button(
    name: String,
    private var _disabled: Boolean = false
) extends RxElement {

  def isActive: Boolean   = !_disabled
  def isDisabled: Boolean = _disabled

  def active: Button = {
    _disabled = false
    this
  }
  def disable: Button = {
    _disabled = true
    this
  }

  def render: HtmlElement = {
    button(_type -> "button", _class -> "btn", disabled.when(_disabled), name)
  }
}

object Button extends LogSupport {
  def primary(name: String)   = button(name, cls -> "btn btn-primary")
  def secondary(name: String) = button(name, cls -> "btn btn-secondary")
  def success(name: String)   = button(name, cls -> "btn btn-success")
  def danger(name: String)    = button(name, cls -> "btn btn-danger")
  def warning(name: String)   = button(name, cls -> "btn btn-warning")
  def info(name: String)      = button(name, cls -> "btn btn-info")
  def light(name: String)     = button(name, cls -> "btn btn-light")
  def dark(name: String)      = button(name, cls -> "btn btn-dark")
  def link(name: String)      = button(name, cls -> "btn btn-link")
}
