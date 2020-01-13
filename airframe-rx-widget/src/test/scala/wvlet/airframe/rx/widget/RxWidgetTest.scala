package wvlet.airframe.rx.widget

import wvlet.airspec._
import wvlet.airframe.rx.widget.ui.Layout
import wvlet.airframe.rx.widget.ui.bootstrap._
import org.scalajs.dom

object RxWidgetTest {}

class RxWidgetTest extends AirSpec {
  import RxWidgetTest._

  test("render nested components") {
    val elem = Layout.of(
      Layout.div(
        Button.primary("click me")
      )
    )
    val dom = RxDOM.mount(elem)
    info(dom)
  }

}
