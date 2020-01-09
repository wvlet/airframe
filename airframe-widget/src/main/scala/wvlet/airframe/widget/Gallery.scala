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
import wvlet.airframe.widget.components.{Button, Container, Layout, Modal}
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
    alertGallery,
    modalGallery,
    gridGallery
  )

  def demo(title: String, main: RxElement, code: String): RxElement = {
    Container.of(
      Layout.h4(title),
      Layout.row.withBorder.withRoundedCorner(
        Layout.col(main),
        Layout.col(Layout.scalaCode(code))
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

    val disabledButtons = buttons.map(_.disable)

    Container.of(
      demo(
        "Buttons",
        Container.ofList(buttons),
        """import wvlet.airframe.widget._
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
      ),
      demo(
        "Disabled Buttons",
        Container.ofList(disabledButtons),
        """import wvlet.airframe.widget._
          |
          |Button.primary("Primary").disable
          |Button.secondary("Secondary").disable
          |Button.success("Success").disable
          |Button.danger("Danger").disable
          |Button.warning("warning").disable
          |Button.info("Info").disable
          |Button.light("Light").disable
          |Button.dark("Dark").disable
          |Button.link("Link").disable""".stripMargin
      )
    )
  }

  def alertGallery = Container.of {
    demo(
      "Alerts",
      Container.of(
        Layout.alertPrimary("A simple alert!"),
        Layout.alertSecondary("A simple alert!"),
        Layout.alertSuccess("A simple alert!"),
        Layout.alertDanger("A simple alert!"),
        Layout.alertWarning("A simple alert!"),
        Layout.alertInfo("A simple alert!"),
        Layout.alertInfo("A simple alert!"),
        Layout.alertLight("A simple alert!"),
        Layout.alertDark("A simple alert!")
      ),
      """import wvlet.airframe.widget._
        |
        |Layout.alertPrimary("A simple alert!")
        |Layout.alertSecondary("A simple alert!")
        |Layout.alertSuccess("A simple alert!")
        |Layout.alertDanger("A simple alert!")
        |Layout.alertWarning("A simple alert!")
        |Layout.alertInfo("A simple alert!")
        |Layout.alertInfo("A simple alert!")
        |Layout.alertLight("A simple alert!")
        |Layout.alertDark("A simple alert!")
        |""".stripMargin
    )
  }

  def modalGallery = Container.of(
    demo("Modal", new Modal(title = "Modal Demo").apply("Modal body text goes here"), "src")
  )

  def gridGallery = Container.of {
    Layout.row(
      Layout.colSm { "One of three columns" },
      Layout.colSm { "One of three columns" },
      Layout.colSm { "One of three columns" }
    )
  }

}
