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
import wvlet.airframe.rx.widget.ui.bootstrap._

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

    content.mountTo(main)
  }

  def galleryFrame = RxComponent { content =>
    <div>
      {
      NavBar
        .fixedTop("Airframe") {
          NavBar.navList(
            NavBar.navItemActive("Home"),
            NavBar.navItem("Link"),
            NavBar.navItemDisabled("Disabled")
          )
        }
    }

      {
      containerFluid(
        <div class="row">
          {
          NavBar.sideBarSticky(
            NavBar.navItemActive(
              <a class="nav-link active" href="#">
                Dashboard <span class="sr-only">(current)</span>
              </a>
            ),
            NavBar.navItem(
              <a class="nav-link" href="#">
                Orders
              </a>
            ),
            NavBar.navItem(
              <a class="nav-link" href="#">
                Products
              </a>
            ),
            NavBar.navItem(
              <a class="nav-link" href="#">
                Integrations
              </a>
            ),
            NavBar.navItem(
              Button
                .primary("Click Me!")
                .onClick(e => logger.info("Clicked"))
            )
          )
        }

          <main role="main" class="col-md-10 ml-md-auto">
            <h2>Airframe RxWidget Gallery</h2>
            {content}
          </main>
        </div>
      )
    }
    </div>
  }

  def gallery = Seq(
    componentGallery,
    elementGallery,
    browserGallery,
    buttonGallery,
    alertGallery,
    modalGallery,
    gridGallery
  )

  def componentGallery = {
    Layout.of(
      <div>
        <h4>RxComponent</h4>
        <p>RxComponent is a unit to define a reactive widget that can enclose the other components or elements.</p>
        {
        Layout.scalaCode {
          s"""import scala.xml
             |import wvlet.airframe.rx.widget._
             |
             |class MyComponent extends RxComponent {
             |  def render(content: xml.Node): xml.Node =
             |    <div class="main">
             |      <h2>Hello Airframe Rx Widget!</h2>
             |      {content}
             |    </div>
             |}
             |""".stripMargin
        }
      }
      <p>Here is a handy-syntax to define a new component:</p>
        {
        Layout.scalaCode {
          """// Short-hand notation for defining a new RxComponent at ease
            |RxComponent { content =>
            |  <div class="main">
            |    <h2>Hello Airframe Rx Widget!</h2>
            |    {content}
            |  </div>
            |}
            |""".stripMargin
        }
      }
      </div>
    )
  }

  def elementGallery = {
    Layout.of(
      <div>
      <h4>RxElement</h4>
      {
        Layout.scalaCode(
          s"""import wvlet.airframe.rx.widget._
           |
           |class MyButton(name:String) extends RxElement {
           |  def render: xml.Node = <button class="button">{name}</button>
           |}
           |
           |// Short-hand notation
           |def newButton(name:String): RxElement =
           |  RxElement{ <button class="button">{name}</button> }
           |""".stripMargin
        )
      }
      </div>
    )
  }

  def demo(title: String, main: RxElement, code: String): RxElement = {
    containerFluid(
      Layout.h4(title),
      row.withBorder.withRoundedCorner(
        col(main),
        col(Layout.scalaCode(code))
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
      Layout.of(buttons: _*),
      """import wvlet.airframe.rx.widget.ui.bootstrap._
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

  def alertGallery = demo(
    "Alerts",
    Layout.of(
      Alert.primary("A simple alert!"),
      Alert.secondary("A simple alert!"),
      Alert.success("A simple alert!"),
      Alert.danger("A simple alert!"),
      Alert.warning("A simple alert!"),
      Alert.info("A simple alert!"),
      Alert.light("A simple alert!"),
      Alert.dark("A simple alert!")
    ),
    """import wvlet.airframe.rx.widget.ui.bootstrap._
        |
        |Alert.primary("A simple alert!")
        |Alert.secondary("A simple alert!")
        |Alert.success("A simple alert!")
        |Alert.danger("A simple alert!")
        |Alert.warning("A simple alert!")
        |Alert.info("A simple alert!")
        |Alert.light("A simple alert!")
        |Alert.dark("A simple alert!")
        |""".stripMargin
  )

  def modalGallery = demo(
    "Modal",
    Modal
      .default(title = "ModalDemo")
      .addStyle("display: block")
      .addStyle("position: relative")
      .withFooter(
        <div>
          <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
          <button type="button" class="btn btn-primary">Save changes</button>
        </div>
      ).apply(
        <b>Modal body text goes here</b>
      ),
    """Modal
      |  .default(title = "ModalDemo")
      |  .addStyle("display: block")
      |  .addStyle("position: relative") {
      |  .withFooter(
      |    <div>
      |      <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
      |      <button type="button" class="btn btn-primary">Save changes</button>
      |    </div>
      |  ).apply(
      |    <b>Modal body text goes here</b>
      |  )
      |""".stripMargin
  )

  def gridGallery = demo(
    "Grid",
    row(
      col { "One of three columns" },
      col { "One of three columns" },
      col { "One of three columns" }
    ),
    """row(
        |  col { "One of three columns" },
        |  col { "One of three columns" },
        |  col { "One of three columns" }
        |)""".stripMargin
  )

  def browserGallery = demo(
    "Browser Info",
    Layout.p(s"browser url: ${Browser.url}"),
    """Layout.p(s"browser url: ${Browser.url}")""".stripMargin
  )
}
