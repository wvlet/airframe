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
package wvlet.airframe.http.rx.widget.ui.bootstrap

import wvlet.airframe.http.rx.html.RxComponent
import wvlet.airframe.http.rx.html.all._

/**
  */
object Alert {
  private def newAlert(style: String) =
    RxComponent { content =>
      val cls = s"alert ${style}"
      div(role -> "alert", _class -> cls, content)
    }

  def primary   = newAlert("alert-primary")
  def secondary = newAlert("alert-secondary")
  def success   = newAlert("alert-success")
  def danger    = newAlert("alert-danger")
  def warning   = newAlert("alert-warning")
  def info      = newAlert("alert-info")
  def light     = newAlert("alert-light")
  def dark      = newAlert("alert-dark")
}
