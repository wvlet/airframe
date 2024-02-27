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
package wvlet.airframe.http.rx.html

import org.scalajs.dom.{HTMLElement, document}
import wvlet.airframe.rx.{Cancelable, Rx, html}
import wvlet.airframe.rx.html.{DOMRenderer, Embedded, RxElement}
import wvlet.airspec.AirSpec
import wvlet.airframe.rx.html.*
import wvlet.airframe.rx.html.all.*
import wvlet.airframe.rx.html.embedAsNode

/**
  */
object RxRenderingTest extends AirSpec {

  private def render(v: Any): (HTMLElement, Cancelable) = {
    val (n, c) = v match {
      case rx: Rx[RxElement] @unchecked =>
        DOMRenderer.createNode(div(rx))
      case other: RxElement =>
        DOMRenderer.createNode(other)
    }
    (n.asInstanceOf[HTMLElement], c)
  }

  test("rendering RxOptionVar") {
    val v = Rx.optionVariable[String](None)

    val (n, c) = render {
      v.transform {
        case Some(x) => div(x)
        case None    => span("N/A")
      }
    }

    n.outerHTML shouldBe """<div><span>N/A</span></div>"""
    v := Some("Hello")
    n.outerHTML shouldBe """<div><div>Hello</div></div>"""
  }

  test("rendering RxOption with map") {
    val title = Rx.optionVariable(Some("home"))

    val (n, c) = render {
      div(
        "menu:",
        title.map { x =>
          b(x)
        }
      )
    }

    n.outerHTML shouldBe """<div>menu:<b>home</b></div>"""
    title := None
    n.outerHTML shouldBe """<div>menu:</div>"""
  }

  test("beforeRender/beforeUnmount") {
    var a                = 0
    var b                = 0
    var afterRenderCount = 0

    val v = Rx.variable(1)
    val r = new RxElement {
      override def beforeRender: Unit = {
        a += 1
      }

      override def onMount: Unit = {
        afterRenderCount += 1
      }
      override def beforeUnmount: Unit = {
        b += 1
      }
      override def render: RxElement = span(v.map { x => s"hello ${x}" })
    }

    a shouldBe 0
    afterRenderCount shouldBe 0
    val (n, c) = render(r)
    n.outerHTML shouldBe "<span>hello 1</span>"
    a shouldBe 1
    afterRenderCount shouldBe 1
    b shouldBe 0

    // Updating inner element should not trigger on render
    v := 2
    a shouldBe 1
    afterRenderCount shouldBe 1
    b shouldBe 0
    n.outerHTML shouldBe "<span>hello 2</span>"

    // unmounting
    c.cancel
    a shouldBe 1
    afterRenderCount shouldBe 1
    b shouldBe 1
  }

  test("beforeRender/beforeUnmount for RxElement(...)") {
    var a                = 0
    var b                = 0
    var afterRenderCount = 0

    val v = Rx.variable(1)
    val r = new RxElement {
      override def beforeRender: Unit = {
        a += 1
      }

      override def onMount: Unit = {
        afterRenderCount += 1
      }
      override def beforeUnmount: Unit = {
        b += 1
      }
      override def render: RxElement = span(v.map { x => s"hello ${x}" })
    }

    a shouldBe 0
    afterRenderCount shouldBe 0

    val rw     = RxElement(r)
    val (n, c) = render(rw)
    n.outerHTML shouldBe "<span>hello 1</span>"
    a shouldBe 1
    afterRenderCount shouldBe 1
    b shouldBe 0

    // Updating inner element should not trigger on render
    v := 2
    a shouldBe 1
    afterRenderCount shouldBe 1
    b shouldBe 0
    n.outerHTML shouldBe "<span>hello 2</span>"

    // unmounting
    c.cancel
    a shouldBe 1
    afterRenderCount shouldBe 1
    b shouldBe 1
  }

  test("nested beforeRender/beforeUnmount") {
    var a               = false
    var b               = false
    var afterRenderFlag = false

    var a1               = false
    var b1               = false
    var afterRenderFlag1 = false

    val nested = new RxElement {
      override def beforeRender: Unit = {
        a1 = true
      }
      override def onMount: Unit = {
        afterRenderFlag = true
      }
      override def beforeUnmount: Unit = {
        b1 = true
      }
      override def render: RxElement = span("nested")
    }

    val r = new RxElement {
      override def beforeRender: Unit = {
        a = true
      }
      override def onMount: Unit = {
        afterRenderFlag1 = true
      }
      override def beforeUnmount: Unit = {
        b = true
      }
      override def render: RxElement = span(nested)
    }

    val (n, c) = render(r)
    a shouldBe true
    b shouldBe false
    afterRenderFlag shouldBe true
    a1 shouldBe true
    b1 shouldBe false
    afterRenderFlag shouldBe true

    c.cancel
    b shouldBe true
    b1 shouldBe true
    afterRenderFlag shouldBe true
    afterRenderFlag1 shouldBe true
  }

  test("rendering attributes with Rx") {
    val a = Rx.variable("primary")
    val e = new RxElement {
      override def render: RxElement = div(
        cls -> a
      )
    }
    val (n, c) = render(e)
    n.outerHTML shouldBe """<div class="primary"></div>"""

    a := "secondary"
    n.outerHTML shouldBe """<div class="secondary"></div>"""
    c.cancel
  }

  test("rendering whole attributes with Rx") {
    val a = Rx.variable("primary")
    val e = new RxElement {
      override def render: RxElement = div(
        a.map { x => cls -> x }
      )
    }
    val (n, c) = render(e)
    n.outerHTML shouldBe """<div class="primary"></div>"""

    a := "secondary"
    n.outerHTML shouldBe """<div class="secondary"></div>"""
    c.cancel
  }

  test("rendering attribute value with Rx") {
    val color = Rx.variable("white")

    val e = new RxElement {
      override def render: RxElement = div(
        // This needs to be updated when color variable is changed
        style -> color.map { x => s"color: ${x};" },
        "message"
      )
    }

    val (n, c) = render(e)
    n.outerHTML shouldBe """<div style="color: white;">message</div>"""
    color := "black"
    n.outerHTML shouldBe """<div style="color: black;">message</div>"""
    c.cancel
  }

  test("rendering multiple attribute values with Rx") {
    val color = Rx.variable("white")

    val e = new RxElement {
      override def render: RxElement = div(
        // This needs to be updated when color variable is changed
        color.map { x =>
          Seq(
            style -> s"color: ${x}",
            cls   -> s"color-${x}"
          )
        }
      )
    }

    val (n, c) = render(e)
    n.outerHTML shouldBe """<div style="color: white;" class="color-white"></div>"""
    color := "black"
    n.outerHTML shouldBe """<div style="color: black;" class="color-black"></div>"""
    c.cancel
  }

  test("render attributes with onMount hook") {
    var updated = false

    def findSpan000 = Option(document.getElementById("span000"))

    val label = new RxElement() {
      override def onMount: Unit = {
        logger.debug(s"onRender span: ${findSpan000}")
        findSpan000.foreach { e =>
          e.setAttribute("class", "active")
          updated = true
        }
      }
      override def render: RxElement = {
        logger.debug(s"render span: ${findSpan000}")
        span(id -> "span000")
      }
    }

    val main = new RxElement {
      override def onMount: Unit = {
        logger.debug("onRender main")
      }
      override def render: RxElement = {
        logger.debug(s"render main: ${findSpan000}")
        div(
          label
        )
      }
    }
    val c = main.renderTo("main")
    updated shouldBe true
  }

  test("call onMount hook in nested RxElements") {
    val page = Rx.variable("main")

    var topLevelOnMountCallCount = 0
    var nestedOnMountCallCount   = 0
    var foundElement             = false

    object infoPage extends RxElement {
      override def onMount: Unit = {
        nestedOnMountCallCount += 1
        Option(org.scalajs.dom.document.getElementById("id001")).collect { case e: HTMLElement =>
          foundElement = true
        }
      }
      override def render: RxElement = div(id -> "id001", "render: info")
    }

    object nestedPage extends RxElement() {
      override def onMount: Unit = {
        topLevelOnMountCallCount += 1
      }

      override def render = page.map {
        case "main" =>
          div("main")
        case "info" =>
          infoPage
      }
    }

    val c = nestedPage.renderTo("main")
    page := "info"
    org.scalajs.dom.document.getElementById("id001") shouldMatch { case e: HTMLElement =>
      e.innerHTML shouldContain "render: info"
    }
    topLevelOnMountCallCount shouldBe 1
    nestedOnMountCallCount shouldBe 1
    foundElement shouldBe true
  }

  test("refresh attribute with RxVar") {
    val show = Rx.variable(true)
    val e = new RxElement {
      override def render: RxElement = {
        div(
          show.when(_ == true).map(_ => cls += "active")
        )
      }
    }
    val (n, c) = render(e)
    n.outerHTML shouldBe """<div class="active"></div>"""
    show := false
    n.outerHTML shouldBe """<div></div>"""
    show := true
    n.outerHTML shouldBe """<div class="active"></div>"""
  }

  test("append cls attribute") {
    val selected = Rx.variable("home")
    val e = new RxElement {
      override def render: RxElement = {
        div(
          cls -> "item",
          selected.when(_ == "home").map { x =>
            cls += "active"
          },
          cls += "text-primary",
          selected
        )
      }
    }

    val (n, c) = render(e)
    n.outerHTML shouldBe """<div class="item active text-primary">home</div>"""
    selected := "about"
    // The atrribute should be removed properly
    n.outerHTML shouldBe """<div class="item text-primary">about</div>"""
  }

  test("append style attribute") {
    val selected = Rx.variable("home")
    val e = new RxElement {
      override def render: RxElement = {
        div(
          style -> "color: white;",
          selected.when(_ == "home").map(_ => style += "font-size: 10px;"),
          selected
        )
      }
    }

    val (n, c) = render(e)
    n.outerHTML shouldBe """<div style="color: white; font-size: 10px;">home</div>"""
    selected := "about"
    // The atrribute should be removed properly
    n.outerHTML shouldBe """<div style="color: white;">about</div>"""
  }

  test("append and completely remove cls attribute") {
    val selected = Rx.variable("home")
    val e = new RxElement {
      override def render: RxElement = {
        div(
          selected.when(_ == "home").map(x => cls += "active"),
          selected
        )
      }
    }

    val (n, c) = render(e)
    n.outerHTML shouldBe """<div class="active">home</div>"""
    selected := "about"
    // The atrribute should be totally removed
    n.outerHTML shouldBe """<div>about</div>"""
  }

}
