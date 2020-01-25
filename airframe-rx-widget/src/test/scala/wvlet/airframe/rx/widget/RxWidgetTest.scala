package wvlet.airframe.rx.widget

import org.scalajs.dom
import wvlet.airframe.rx.Rx
import wvlet.airframe.rx.html.all._
import wvlet.airframe.rx.html.{DOMRenderer, Embedded, RxComponent, RxElement}
import wvlet.airframe.rx.widget.ui.Layout
import wvlet.airframe.rx.widget.ui.bootstrap._
import wvlet.airspec._

object RxWidgetTest {}

class RxWidgetTest extends AirSpec {

  private def renderTo(node: dom.Node, elem: RxElement): Unit = {
    DOMRenderer.renderTo(node, elem)
  }

  private def render(elem: RxElement): String = {
    val (dom, c) = DOMRenderer.render(elem)
    val html = dom match {
      case x: org.scalajs.dom.Element =>
        x.outerHTML
      case _ =>
        dom.innerText
    }
    info(html)
    c.cancel // cleanup
    html
  }

  test("render nested components") {
    val elem = Layout.of(
      div(
        Button.primary("click me")
      )
    )
    val html = render(elem)
    html.contains("btn btn-primary") shouldBe true
  }

  test("reuse component") {
    val myCode: RxComponent = RxComponent { content =>
      pre(code(content))
    }

    val node: RxElement = myCode("import wvlet")
    val html            = render(node)
    html shouldBe "<pre><code>import wvlet</code></pre>"
  }

  test("render nested DOM element") {
    val elem = div(
      Embedded(dom.document.createElement("main"))
    )
    val html = render(elem)
    html.contains("<main>") shouldBe true
  }

  test("render buttons with click action") {
    val elem = Button.primary("my button")(onclick { () =>
      debug("clicked")
    })
    val html = render(elem)
    html.contains("btn btn-primary") shouldBe true
  }

  test("render disabled buttons") {
    val elem = Button.secondary("my button").disable
    val html = render(elem)
    html.contains("disabled") shouldBe true
  }

  test("Apply Rx variable change") {
    val node = dom.document.createElement("div")
    val v    = Rx.variable(1)
    val elem = div(v.map(x => x))
    renderTo(node, elem)
    node.innerHTML shouldBe "<div>1</div>"
    v := 2
    node.innerHTML shouldBe "<div>2</div>"
  }

  test("Update the local dom element upon Rx variable change") {
    val node = dom.document.createElement("div")
    val v    = Rx.variable("Home")
    val content = div(
      v.map { selected =>
        ul(
          Seq("Home", "Blog").map { page =>
            li(_class -> { if (page == selected) Some("active") else None }, page)
          }
        )
      }
    )
    renderTo(node, content)
    node.innerHTML shouldBe """<div><ul><li class="active">Home</li><li>Blog</li></ul></div>"""
    v := "Blog"
    node.innerHTML shouldBe """<div><ul><li>Home</li><li class="active">Blog</li></ul></div>"""
  }

  case class Label(id: String, name: String)

  test("Render Rx as top-level node") {
    val currentPage = Rx.variable("home")
    val d = currentPage.map { page =>
      p(s"page: ${page}")
    }

    val node = dom.document.createElement("div")
    renderTo(node, d)
    node.innerHTML shouldBe "<p>page: home</p>"
  }

}
