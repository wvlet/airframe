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

import wvlet.airframe.rx.html.RxElement
import wvlet.airframe.rx.html._
import wvlet.airframe.rx.html.all.{button, cls, disabled, tpe}
import wvlet.log.LogSupport

/**
  */
case class Button(
    name: String,
    private val primaryClass: String = "btn-primary",
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

  def render: RxElement = {
    button(tpe -> "button", cls -> s"btn ${primaryClass}", disabled.when(_disabled), name)
  }
}

object Button extends LogSupport {
  def primary(name: String)   = Button(name)
  def secondary(name: String) = Button(name, "btn-secondary")
  def success(name: String)   = Button(name, "btn-success")
  def danger(name: String)    = Button(name, "btn-danger")
  def warning(name: String)   = Button(name, "btn-warning")
  def info(name: String)      = Button(name, "btn-info")
  def light(name: String)     = Button(name, "btn-light")
  def dark(name: String)      = Button(name, "btn-dark")
  def link(name: String)      = Button(name, "btn-link")
}
