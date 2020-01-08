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
package wvlet.airframe.widget
import org.scalajs.dom
import org.scalajs.dom.document
import wvlet.airframe.widget.components.{Button, Container, Layout}
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
    main.appendChild(gallery.render)
  }

  def gallery = Container.of(
    buttonGallery,
    gridGallery
  )

  def demo(title: String, main: RxElement, code: String): RxElement = {
    Container.of(
      Layout.h3(title),
      Layout.row.withBorder.withRoundedCorner(
        Layout.colSm(main),
        Layout.colSm(Layout.scalaCode(code))
      )
    )
  }

  def buttonGallery = {
    def buttons = Seq(
      Button.primary("Primary"),
      Button.secondary("Secondary"),
      Button.success("Success"),
      Button.danger("Danger"),
      Button.warning("warning"),
      Button.info("Info"),
      Button.light("Light"),
      Button.dark("Dark"),
      Button.link("Link")
    )

    val buttonCode = """import wvlet.airframe.widget._
                  |
                  |Button.primary("Primary")
                  |Button.secondary("Secondary")
                  |Button.success("Success")
                  |Button.danger("Danger")
                  |Button.warning("warning")
                  |Button.info("Info")
                  |Button.light("Light")
                  |Button.dark("Dark")
                  |Button.link("Link")""".stripMargin

    val disabledButtons = buttons.map(_.disable)

    Container.of(
      demo("Buttons", Container.ofList(buttons), buttonCode),
      demo("Disabled Buttons", Container.ofList(disabledButtons), buttonCode)
    )
  }

  def gridGallery = Container.of {
    Layout.row(
      Layout.colSm { "One of three columns" },
      Layout.colSm { "One of three columns" },
      Layout.colSm { "One of three columns" }
    )
  }

}
