package wvlet.airframe.rx.widget

import org.scalajs.dom
import wvlet.airframe.rx.widget.ui.Layout
import wvlet.airframe.rx.widget.ui.bootstrap._
import wvlet.airspec._

object RxWidgetTest {}

class RxWidgetTest extends AirSpec {

  test("render nested components") {
    val elem = Layout.of(
      Layout.div(
        Button.primary("click me")
      )
    )
    val node = dom.document.createElement("div")
    RxDOM.mountTo(node, elem)
    info(node.childNodes)
  }

}
