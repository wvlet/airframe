package wvlet.airframe.rx.widget

import org.scalajs.dom
import wvlet.airframe.rx.Rx
import wvlet.airframe.rx.widget.ui.bootstrap._
import wvlet.airframe.rx.widget.ui.{DomElement, Layout}
import wvlet.airspec._

object RxWidgetTest {}

class RxWidgetTest extends AirSpec {

  private def renderTo(node: dom.Element, elem: RxElement): String = {
    RxDOM.mountTo(node, elem)
    val html = node.innerHTML
    debug(html)
    html
  }

  private def render(elem: RxElement): String = {
    val node = dom.document.createElement("div")
    renderTo(node, elem)
  }

  test("render nested components") {
    val elem = Layout.of(
      Layout.div(
        Button.primary("click me")
      )
    )
    val html = render(elem)
    html.contains("btn btn-primary") shouldBe true
  }

  test("render nested DOM element") {
    val elem = Layout.div(
      DomElement(dom.document.createElement("main"))
    )
    val html = render(elem)
    html.contains("<main>") shouldBe true
  }

  test("render buttons with click action") {
    val elem = Button.primary("my button").onClick(e => debug("clicked"))
    val html = render(elem)
    html.contains("btn btn-primary") shouldBe true
  }

  test("Apply Rx variable change") {
    val node = dom.document.createElement("div")
    val v    = Rx.variable(1)
    val elem = Layout.div(v.map(x => x))
    val html = renderTo(node, elem)
    html shouldBe "<div>1</div>"
    v := 2
    node.innerHTML shouldBe "<div>2</div>"
  }

  test("Update the local dom element upon Rx variable change") {
    val node = dom.document.createElement("div")
    val v    = Rx.variable("Home")
    val content = Layout.div(
      v.map { selected =>
        <ul>{
          Seq("Home", "Blog").map { page =>
            <li class={if (page == selected) Some("active") else None}>{page}</li>
          }
        }</ul>
      }
    )
    val html = renderTo(node, content)
    html shouldBe """<div><ul><li class="active">Home</li><li>Blog</li></ul></div>"""
    v := "Blog"
    node.innerHTML shouldBe """<div><ul><li>Home</li><li class="active">Blog</li></ul></div>"""
  }

}
