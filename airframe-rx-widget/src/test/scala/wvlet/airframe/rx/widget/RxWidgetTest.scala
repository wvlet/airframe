package wvlet.airframe.rx.widget

import org.scalajs.dom
import wvlet.airframe.rx.widget.ui.bootstrap._
import wvlet.airframe.rx.widget.ui.{DomElement, Layout}
import wvlet.airspec._

object RxWidgetTest {}

class RxWidgetTest extends AirSpec {

  private def render(elem: RxElement): String = {
    val node = dom.document.createElement("div")
    RxDOM.mountTo(node, elem)
    val html = node.innerHTML
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

}
