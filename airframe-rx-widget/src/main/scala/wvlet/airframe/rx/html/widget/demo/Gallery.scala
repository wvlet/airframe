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
package wvlet.airframe.rx.html.widget.demo

import org.scalajs.dom
import org.scalajs.dom.document
import wvlet.airframe.rx.Rx
import wvlet.airframe.rx.html.all._
import wvlet.airframe.rx.html.widget.ui.Browser
import wvlet.airframe.rx.html.{DOMRenderer, RxCode, RxComponent, RxElement, extractCode, tags}
import wvlet.log.{LogLevel, LogSupport, Logger}

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  */
@JSExportTopLevel("Gallery")
object Gallery extends LogSupport {
  @JSExport
  def main(): Unit = {
    //Logger.setDefaultLogLevel(LogLevel.DEBUG)
    info(s"started")
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
    DOMRenderer.renderTo(main, galleryFrame)
  }

  def galleryFrame =
    div(
      cls -> "container",
      div(
        cls -> "row",
        div(
          cls -> "col-3",
          div(
            cls -> "sticky-top",
            h6(
              cls -> "dropdown-header",
              "Airframe Gallery"
            ),
            gallery2.map { e =>
              a(
                cls  -> "dropdown-item",
                href -> s"#${e.description}",
                e.description
              )
            }
          )
        ),
        div(
          cls -> "col-9",
          tags.main(
            role -> "main",
            gallery2.map { e =>
              div(
                cls -> "container-fluid",
                a(id -> s"${e.description}"),
                div(
                  cls -> "row my-1",
                  h4(e.description)
                ),
                div(cls -> "row my-1", e.code.rxElements),
                div(
                  cls -> "row my-1",
                  pre(cls -> "w-100", code(cls -> "language-scala rounded", e.code.sourceCode))
                )
              )
            }
          )
        )
      )
    )

  case class Example(description: String, code: RxCode)

  def gallery2: Seq[Example] = Seq(
    elementGallery,
    customElementGallery,
    componentGallery,
    buttonDemo,
    buttonSmallDemo,
    alertsDemo,
    rxTest,
    browserDemo,
    canvasDemo,
    svgDemo
  )

  def buttonDemo: Example = Example(
    "Buttons",
    extractCode(
      button(tpe -> "button", cls -> "btn btn-primary", "primary"),
      button(tpe -> "button", cls -> "btn btn-secondary", "secondary"),
      button(tpe -> "button", cls -> "btn btn-success", "success"),
      button(tpe -> "button", cls -> "btn btn-danger", "danger"),
      button(tpe -> "button", cls -> "btn btn-info", "info"),
      button(tpe -> "button", cls -> "btn btn-warning", "warning"),
      button(tpe -> "button", cls -> "btn btn-light", "light"),
      button(tpe -> "button", cls -> "btn btn-dark", "dark")
    )
  )

  def buttonSmallDemo: Example = Example(
    "Small buttons",
    extractCode {
      import wvlet.airframe.rx.html.all._

      def smallButton(style: String) =
        button(tpe -> "button", cls -> s"btn btn-sm btn-${style}", style)

      div(
        smallButton("primary"),
        smallButton("secondary"),
        smallButton("success"),
        smallButton("danger"),
        smallButton("info"),
        smallButton("warn"),
        smallButton("light"),
        smallButton("dark")
      )
    }
  )

  def alertsDemo = Example(
    "Alerts",
    extractCode(
      div(cls -> "alert alert-primary", role   -> "alert", "alert!"),
      div(cls -> "alert alert-secondary", role -> "alert", "alert!"),
      div(cls -> "alert alert-success", role   -> "alert", "alert!"),
      div(cls -> "alert alert-danger", role    -> "alert", "alert!"),
      div(cls -> "alert alert-info", role      -> "alert", "alert!"),
      div(cls -> "alert alert-warn", role      -> "alert", "alert!"),
      div(cls -> "alert alert-light", role     -> "alert", "alert!"),
      div(cls -> "alert alert-dark", role      -> "alert", "alert!")
    )
  )

  def rxTest = Example(
    "Rx.variable",
    extractCode {
      // Define a reactive variable
      val v = Rx.variable(1)
      div(
        button(
          cls -> "btn btn-primary",
          onclick { e: dom.Event =>
            v.update(_ + 1)
          },
          "Increment"
        ),
        // This code will be triggered when the variable is updated
        v.map(x => span(cls -> "mx-1", s"count: ${x}"))
      )
    }
  )

  def canvasDemo = Example(
    "Canvas",
    extractCode {
      import wvlet.airframe.rx.html.widget.ui.Canvas

      // Creating a new canvas
      val c = Canvas.newCanvas(50, 50)
      import c._
      context.fillStyle = "#99CCFF"
      context.fillRect(0, 0, c.canvas.width, c.canvas.height)
      context.strokeStyle = "#336699"
      context.beginPath()
      context.arc(25, 25, 15, 0, 2 * math.Pi)
      context.stroke()
      c
    }
  )

  def svgDemo = Example(
    "SVG",
    extractCode {
      import wvlet.airframe.rx.html.svgAttrs._
      import wvlet.airframe.rx.html.svgTags._

      val circleColor = Rx.variable("white")

      svg(
        width  -> 50,
        height -> 50,
        rect(x -> 0, y -> 0, width -> 50, height -> 50, fill -> "#336699"),
        circle(
          cx   -> "50%",
          cy   -> "50%",
          r    -> "30%",
          fill -> circleColor,
          onmouseover -> { () =>
            circleColor.set("#99CCFF")
          },
          onmouseout -> { () =>
            circleColor.set("white")
          }
        )
      )
    }
  )

  def browserDemo = Example(
    "Browser info",
    extractCode {
      p(s"Browser url: ${Browser.url}")
    }
  )

  def elementGallery = Example(
    "RxElement",
    extractCode {
      import wvlet.airframe.rx.html.all._

      class MyButton(name: String) extends RxElement {
        def render: RxElement = button(cls -> "btn btn-outline-primary", name)
      }

      new MyButton("Button")
    }
  )

  def customElementGallery = Example(
    "Customize RxElement",
    extractCode {
      import wvlet.airframe.rx.html.all._

      def myButton = button(cls -> "btn btn-outline-primary")

      // Appending a new style and child elements is suppoted with RxElement.apply(...)
      myButton(cls += "btn-sm", "Custom Button")
    }
  )

  def componentGallery = Example(
    "RxComponent",
    extractCode {
      import wvlet.airframe.rx.html.all._

      // RxComponent is the unit of a reactive widget that can enclose other components or elements.
      class MyComponent extends RxComponent {
        def render(content: RxElement): RxElement =
          div(cls -> "main", div(cls -> "alert alert-info", content))
      }

      new MyComponent().render("Hello airframe-rx-html!!")
    }
  )
}
