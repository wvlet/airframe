/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.rx.html

import wvlet.airframe.rx.Rx
import wvlet.airframe.rx.html.all._
import wvlet.airframe.rx.html.svgTags._
import wvlet.airspec.AirSpec

/**
  */
class HtmlTest extends AirSpec {
  test("embedding values") {
    div(
      1,
      true,
      false,
      10L,
      "hello",
      null,
      Nil,
      1.toShort,
      1.toByte,
      'a',
      1.0f,
      1.0,
      Some(1),
      None,
      Rx.variable(1),
      Iterable("a", "b", "c"),
      (cls -> "main").when(1 == 1),
      (cls -> "main2").unless(1 == 1),
      (cls -> "main").when(1 != 1),
      (cls -> "main2").unless(1 != 1)
    )
  }

  test("embedding attributes") {
    div(
      cls -> 1,
      cls -> true,
      cls -> 10L,
      cls -> "hello",
      cls -> 1.0f,
      cls -> 1.0,
      cls -> Some(1),
      cls -> None,
      cls -> Rx.variable(1),
      cls -> Iterable(1, 2, 3),
      onclick -> { () =>
        "hello"
      },
      onclick -> { (event: Any) =>
        "hello"
      }
    )
  }

  test("html composition") {
    import tags.font
    div(
      a(href -> "https://wvlet.org/"),
      area(),
      b("hello"),
      blockquote("quote"),
      body("main"),
      br("border"),
      button("button"),
      canvas(),
      caption(),
      center(),
      cite(),
      code(),
      dd(),
      del(),
      details(),
      dfn(),
      dialog(),
      div(),
      dl(),
      dt(),
      em(),
      embed(),
      fieldset(),
      figcaption(),
      figure(),
      font(),
      footer(),
      form(),
      frame(),
      frameset(),
      h1(),
      h2(),
      h3(),
      h4(),
      h5(),
      h6(),
      hr(),
      html(),
      i(),
      iframe(),
      img()
    )
  }

  test("attribute modification test") {
    div(
      id    -> "test",
      style -> "color: black;",
      cls   -> "container"
    )

    div(
      id -> "test",
      style += "color: black;",
      cls += "container"
    )

    div(
      style.noValue
    )

    val d = div()
    d(style           -> "width: 10px;")
    d.addModifier(cls -> "test")
  }

  test("create new tags") {
    div(
      tagOf("mytag", Namespace.xhtml)(
        attr("myattr", Namespace.xhtml) -> "attr_value",
        attributeOf("myattr2")          -> "v2"
      )
    )
  }

  test("extra tags") {
    import tags_extra._
    div(
      abbr(),
      acronym(),
      address(),
      applet(),
      article(),
      aside(),
      audio(),
      base(),
      basefont(),
      bdi(),
      bdo(),
      big(),
      col(),
      colgroup(),
      data(),
      datalist(),
      dir(),
      head(),
      header(),
      mark(),
      output(),
      progress(),
      ruby(),
      samp(),
      style(),
      time(),
      title(),
      track(),
      track(),
      tt(),
      _var(),
      video()
    )
  }

  test("data attrs") {
    div(
      data("test-value") -> "xxx"
    )

  }

  test(s"svg tags") {
    import svgTags._
    import svgAttrs._
    svg(
      circle(
        color -> ""
      )
    )
  }
}
