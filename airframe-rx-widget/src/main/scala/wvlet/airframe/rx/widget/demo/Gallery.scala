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
package wvlet.airframe.rx.widget.demo
import org.scalajs.dom
import org.scalajs.dom.document
import wvlet.airframe.rx.widget.ui._
import wvlet.airframe.rx.widget.{RxComponent, RxElement}
import wvlet.log.{LogLevel, LogSupport, Logger}

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.xml.Node

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
    val content = galleryFrame(gallery: _*)

    main.appendChild(content.toDOM)
  }

  def galleryFrame = new RxComponent {
    override def render(content: Node*): Node =
      <div>
        {
        NavBar
          .fixedTop("Airframe").render {
            <div class="collapse navbar-collapse" id="navbarCollapse">
            <ul class="navbar-nav mr-auto">
              <li class="nav-item active">
                <a class="nav-link" href="#">Home <span class="sr-only">(current)</span></a>
              </li>
              <li class="nav-item">
                <a class="nav-link" href="#">Link</a>
              </li>
              <li class="nav-item">
                <a class="nav-link disabled" href="#" tabindex="-1" aria-disabled="true">Disabled</a>
              </li>
            </ul>
          </div>
          }
      }

        <div class="container-fluid">
          <div class="row">
            {
        SideBar.sticky.render(
          <ul class="nav flex-column">
                <li class="nav-item">
                  <a class="nav-link active" href="#">
                    <span data-feather="home"></span>
                    Dashboard <span class="sr-only">(current)</span>
                  </a>
                </li>
                <li class="nav-item">
                  <a class="nav-link" href="#">
                    <span data-feather="file"></span>
                    Orders
                  </a>
                </li>
                <li class="nav-item">
                  <a class="nav-link" href="#">
                    <span data-feather="shopping-cart"></span>
                    Products
                  </a>
                </li>
                <li class="nav-item">
                  <a class="nav-link" href="#">
                    <span data-feather="users"></span>
                    Customers
                  </a>
                </li>
                <li class="nav-item">
                  <a class="nav-link" href="#">
                    <span data-feather="bar-chart-2"></span>
                    Reports
                  </a>
                </li>
                <li class="nav-item">
                  <a class="nav-link" href="#">
                    <span data-feather="layers"></span>
                    Integrations
                  </a>
                </li>
              </ul>
        )
      }

            <main role="main" class="col-md-10 ml-md-auto">
              <h2>Airframe Widget Gallery</h2>
            {content}
            </main>
        </div>
      </div>
    </div>
  }

  def gallery = Seq(
    browserGallery,
    buttonGallery,
    alertGallery,
    modalGallery,
    gridGallery
  )

  def demo(title: String, main: RxElement, code: String): RxElement = {
    Layout.containerFluid(
      Layout.h4(title),
      Layout.row.withBorder.withRoundedCorner(
        Layout.col(main),
        Layout.col(Layout.scalaCode(code))
      )
    )
  }

  def buttonGallery = {
    def buttons: Seq[Button] = Seq(
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

    demo(
      "Buttons",
      Layout.list(buttons: _*),
      """import wvlet.airframe.rx.widget._
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
    )
//      demo(
//        "Disabled Buttons",
//        Container.ofList(disabledButtons),
//        """import wvlet.airframe.rx.widget._
//          |
//          |Button.primary("Primary").disable
//          |Button.secondary("Secondary").disable
//          |Button.success("Success").disable
//          |Button.danger("Danger").disable
//          |Button.warning("warning").disable
//          |Button.info("Info").disable
//          |Button.light("Light").disable
//          |Button.dark("Dark").disable
//          |Button.link("Link").disable""".stripMargin
//      )
  }

  def alertGallery = Container.of {
    demo(
      "Alerts",
      Layout.list(
        Layout.alertPrimary("A simple alert!"),
        Layout.alertSecondary("A simple alert!"),
        Layout.alertSuccess("A simple alert!"),
        Layout.alertDanger("A simple alert!"),
        Layout.alertWarning("A simple alert!"),
        Layout.alertInfo("A simple alert!"),
        Layout.alertLight("A simple alert!"),
        Layout.alertDark("A simple alert!")
      ),
      """import wvlet.airframe.rx.widget._
        |
        |Layout.alertPrimary("A simple alert!")
        |Layout.alertSecondary("A simple alert!")
        |Layout.alertSuccess("A simple alert!")
        |Layout.alertDanger("A simple alert!")
        |Layout.alertWarning("A simple alert!")
        |Layout.alertInfo("A simple alert!")
        |Layout.alertLight("A simple alert!")
        |Layout.alertDark("A simple alert!")
        |""".stripMargin
    )
  }

  def modalGallery = Container.of(
    demo(
      "Modal",
      new Modal(title = "Modal Demo").apply("Modal body text goes here"),
      """new Modal(title = "Modal Demo")
        |  .apply("Modal body text goes here")""".stripMargin
    )
  )

  def gridGallery =
    demo(
      "Grid",
      Layout.row(
        Layout.colSm { "One of three columns" },
        Layout.colSm { "One of three columns" },
        Layout.colSm { "One of three columns" }
      ),
      """Layout.row(
        |  Layout.colSm { "One of three columns" },
        |  Layout.colSm { "One of three columns" },
        |  Layout.colSm { "One of three columns" }
        |)""".stripMargin
    )

  def browserGallery =
    demo(
      "Browser Info",
      Layout.p(s"browser url: ${Browser.url}"),
      """Layout.p(s"browser url: ${Browser.url}")""".stripMargin
    )
}
