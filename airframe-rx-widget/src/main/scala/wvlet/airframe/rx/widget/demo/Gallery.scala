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
import wvlet.airframe.rx.Rx
import wvlet.airframe.rx.html.{RxComponent, _}
import wvlet.airframe.rx.html.all._
import wvlet.airframe.rx.widget.ui._
import wvlet.airframe.rx.widget.ui.bootstrap._
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
    val content: RxElement = galleryFrame(gallery: _*)
    content.mountTo(main)
  }

  def galleryFrame = RxComponent { content =>
    div(
      NavBar
        .fixedTop("Airframe") {
          NavBar.navList(
            NavBar.navItemActive("Home"),
            NavBar.navItem("Link"),
            NavBar.navItemDisabled("Disabled")
          )
        },
      containerFluid(
        row(
          NavBar.sideBarSticky(
            NavBar.navItemActive(
              a(_class -> "nav-link active", href -> "#", "Components ", span(_class -> "sr-only"), "(current)")
            ),
            NavBar.navItem(a(_class -> "nav-link", href -> "#", "Layout")),
            NavBar.navItem(a(_class -> "nav-link", href -> "#", "Gallery")),
            NavBar.navItem(a(_class -> "nav-link", href -> "#", "Reactive")),
            NavBar.navItem(
              button("Click Me!", cls -> "btn btn-primary", onclick -> { () =>
                logger.info("Clicked")
              })
            )
          ),
          tags.main(role -> "main", _class -> "col-md-10 ml-md-auto", h2("Airframe RxWidget Gallery"), content)
        )
      )
    )
  }

  def gallery: Seq[HtmlNode] = Seq(
    componentGallery,
    elementGallery,
    reactiveTest,
    canvasGallery,
    browserGallery,
    buttonGallery,
    //buttonDisabledGallery,
    alertGallery,
    modalGallery,
    gridGallery
  )

  def componentGallery = {
    div(
      h4("RxComponent"),
      p("RxComponent is the unit of a reactive widget that can enclose other components or elements."),
      Layout.scalaCode(
        code(
          s"""import wvlet.airframe.rx.widget._
           |
           |class MyComponent extends RxComponent {
           |  def render(content: xml.Node): xml.Node =
           |    div(cls->"main",
           |      h2("Hello Airframe Rx Widget!"),
           |      content
           |    )
           |}
           |""".stripMargin
        )
      ),
      p("Here is a handy-syntax to define a new component:"),
      Layout.scalaCode(
        """// Short-hand notation for defining a new RxComponent at ease
          |RxComponent { content =>
          |  <div class="main">
          |    <h2>Hello Airframe Rx Widget!</h2>
          |    {content}
          |  </div>
          |}
          |""".stripMargin
      )
    )
  }

  def elementGallery = {
    div(
      h4("RxElement"),
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
    )
  }

  def demo(title: String, main: HtmlNode, code: String): HtmlNode = {
    containerFluid(
      h4(title),
      row( // .withBorder.withRoundedCorner(
        bootstrap.col(main),
        bootstrap.col(Layout.scalaCode(code))
      )
    )
  }

  def buttons: Seq[HtmlElement] = Seq(
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

  def buttonGallery = {
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
  }

//  def buttonDisabledGallery = {
//    val disabledButtons = buttons.map(_.disable)
//    demo(
//      "Buttons (disabled)",
//      div(disabledButtons:_*),
//      """import wvlet.airframe.rx.widget.ui.bootstrap._
//        |
//        |Button.primary("Primary").disable
//        |Button.secondary("Secondary").disable
//        |Button.success("Success").disable
//        |Button.danger("Danger").disable
//        |Button.warning("warning").disable
//        |Button.info("Info").disable
//        |Button.light("Light").disable
//        |Button.dark("Dark").disable
//        |Button.link("Link").disable""".stripMargin
//    )
//  }

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
      //.addStyle("display: block")
      //.addStyle("position: relative")
      .withFooter(
        div(
          button(_type -> "button", _class -> "btn btn-secondary", data("dismiss") -> "modal", "Close"),
          button(_type -> "button", _class -> "btn btn-primary", "Save changes")
        )
      ).apply(
        b("Modal body text goes here")
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
      col("One of three columns"),
      col("One of three columns"),
      col("One of three columns")
    ),
    """row(
        |  col { "One of three columns" },
        |  col { "One of three columns" },
        |  col { "One of three columns" }
        |)""".stripMargin
  )

  def browserGallery = demo(
    "Browser Info",
    p(s"browser url: ${Browser.url}"),
    """Layout.p(s"browser url: ${Browser.url}")""".stripMargin
  )

  def canvasGallery = demo(
    "Canvas", {
      val c = Canvas.newCanvas(150, 50)
      import c._
      context.fillStyle = "#336699"
      context.fillRect(0, 0, c.canvas.width, c.canvas.height)
      context.strokeStyle = "#CCCCFF"
      context.beginPath()
      context.arc(25, 25, 15, 0, 2 * math.Pi)
      context.stroke()
      c
    },
    """val c = Canvas.newCanvas(150, 50)
      |import c._
      |context.fillStyle = "#99ccff"
      |context.fillRect(0, 0, c.canvas.width, c.canvas.height)
      |
      |context.strokeStyle = "#CCCCFF"
      |context.beginPath()
      |context.arc(25, 25, 15, 0, 2 * math.Pi)
      |context.stroke()
      |""".stripMargin
  )

  def reactiveTest = {
    val v = Rx.variable(1)
    demo(
      "Rx",
      p(
        v.map(x => s"count: ${x}"),
        Button
          .primary("add")(onclick { e: dom.Event =>
            v.update(_ + 1)
          })
      ),
      """val v = Rx(1)
        |
        |<p>
        |  { v.map(x => s"count: ${x}") }
        |  {
        |   Button
        |    .primary("add")
        |    .onClick { e: dom.Event => v.set(_ + 1) }
        |  }
        |</p>
        |""".stripMargin
    )
  }
}
