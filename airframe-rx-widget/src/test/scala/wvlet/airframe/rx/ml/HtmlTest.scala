package wvlet.airframe.rx.ml
import wvlet.airspec._

import org.scalajs.dom

class HtmlTest extends AirSpec {

  def render(node: html.HtmlElement): Unit = {
    val txt = node.toDOM match {
      case elem: dom.Element =>
        elem.outerHTML
      case other =>
        other.innerText
    }
    info(txt)
    txt
  }

  test("create div") {
    import html._
    val d = div(_class("link"), a(_src("hello")))("hello html!")
    render(d)
  }

}
