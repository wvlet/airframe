package wvlet.airframe.rx.ml
import org.scalajs.dom
import wvlet.airspec._

class HtmlTest extends AirSpec {

  def render(node: html.HtmlElement): String = {
    val txt = node.toDOM match {
      case elem: dom.Element =>
        elem.outerHTML
      case other =>
        other.innerText
    }
    info(txt)
    txt
  }

  import html._

  test("create div") {
    val d = div(_class("link"), a(_src("hello")), "hello html!")
    render(d)
  }

  test("create component") {
    def d(content: ElementModifier*) = {
      div(_class("main"), div(_class("container"), content))
    }

    val x = d(_id("c1"), "hello RxComponent")
    render(x)
  }

}
