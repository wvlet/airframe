package wvlet.airframe.rx.ml
import wvlet.airspec._

class HtmlTest extends AirSpec {

  test("create div") {
    import html._
    val d = div(_class("link"), a(_href("")), "hello html!")
    d.toDOM
  }

}
