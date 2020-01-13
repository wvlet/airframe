package wvlet.airframe.rx.widget

import org.scalajs.dom
import wvlet.airframe.rx.widget.ui.{Canvas, DomElement, Layout}
import wvlet.airframe.rx.widget.ui.bootstrap._
import wvlet.airspec._

object RxWidgetTest {}

class RxWidgetTest extends AirSpec {

  private def render(elem: RxElement): String = {
    val node = dom.document.createElement("div")
    RxDOM.mountTo(node, elem)
    val body = node.childNodes(0)
    val html = DOMRenderer.renderToHTML(body)
    debug(html)
    html
  }

  test("render nested components") {
    val elem = Layout.of(
      Layout.div(
        Button.primary("click me")
      )
    )
    val html = render(elem)
    debug(html)
    html.contains("btn btn-primary") shouldBe true
  }

  test("render nested DOM element") {
    val elem = Layout.div(
      DomElement(dom.document.createElement("main"))
    )
    val html = render(elem)
    debug(html)
    html.contains("<main>") shouldBe true
  }

}
