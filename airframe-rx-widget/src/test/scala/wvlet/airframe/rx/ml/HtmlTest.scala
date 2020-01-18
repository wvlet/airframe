package wvlet.airframe.rx.ml
import wvlet.airspec._

import org.scalajs.dom

class HtmlTest extends AirSpec {

  def render(node: html.HtmlElement): String = {
    val txt = node.toDOM match {
      case elem: dom.Element =>
        elem.innerHTML
      case other: dom.Node =>
        other.innerText
    }
    info(txt)
    txt
  }

  test("create div") {
    import html._
    val d = div(_class("link"), a(_src("hello")), "hello html!")
    render(d)
  }

}
