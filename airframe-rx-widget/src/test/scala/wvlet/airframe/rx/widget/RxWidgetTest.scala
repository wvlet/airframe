package wvlet.airframe.rx.widget

import wvlet.airspec._
import wvlet.airframe.rx.widget.ui.Layout
import wvlet.airframe.rx.widget.ui.bootstrap._

class RxWidgetTest extends AirSpec {

  test("render nested components") {
    val elem = Layout.of(
      Layout.div(
        Button.primary("click me")
      )
    )
    val node = elem.render
    info(node)
  }
}
