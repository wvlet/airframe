package wvlet.airframe.rx.ml
import org.scalajs.dom
import wvlet.airspec._

class HtmlTest extends AirSpec {

  def render(node: html.HtmlElement): String = {
    val txt = node.renderDOM match {
      case (elem: dom.Element, c) =>
        elem.outerHTML
      case (other, c) =>
        other.innerText
    }
    info(txt)
    txt
  }

  import html._

  test("create div") {
    val d = div(cls("link"), a(src("hello")), "hello html!")
    render(d)
  }

  test("create component") {
    def d(content: ElementModifier*) = {
      div(cls("main"), div(cls("container"), content))
    }

    val x = d(id("c1"), "hello RxComponent")
    render(x)
  }

}
