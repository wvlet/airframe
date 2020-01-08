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
import org.scalajs.dom
import org.scalajs.dom.document
import wvlet.log.{LogLevel, LogSupport, Logger}

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  *
  */
@JSExportTopLevel("Gallery")
object Gallery extends LogSupport {
  @JSExport
  def main(): Unit = {
    Logger.setDefaultLogLevel(LogLevel.DEBUG)
    debug(s"started")
    initializeUI
  }

  def initializeUI: Unit = {
    val main = document.getElementById("main") match {
      case null =>
        // For testing
        val elem = dom.document.createElement("div")
        elem.setAttribute("id", "main")
        document.body.appendChild(elem)
      case other => other
    }

    val layout = dom.document.createElement("div")
    layout.textContent = "Hello Airframe Widget!"
    layout.appendChild(Button.default("Default").render)
    layout.appendChild(Button.primary("Primary").render)

    main.appendChild(layout)
  }
}
