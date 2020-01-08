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
package wvlet.airframe.widget.ui
import wvlet.airframe.widget.rx.RxElement

/**
  *
  */
case class Button(name: String, cls: Seq[String] = Seq("btn")) extends RxElement {
  def body                        = <button type="button" class={cls.mkString(" ")}>{name}</button>
  def addClass(className: String) = this.copy(cls = cls :+ className)
}

object Button {
  def default(name: String)   = Button(name).addClass("btn-light")
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
