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

import wvlet.airspec.AirSpec

class UnifiedTagsTest extends AirSpec {
  
  test("should allow using both HTML and SVG elements together with unifiedAll") {
    import unifiedAll.*
    
    // HTML elements with HTML attributes
    val htmlDiv = div(
      id -> "myDiv",
      cls -> "container",
      style -> "color: red;",
      title -> "HTML title attribute"
    )
    
    // SVG elements with SVG attributes
    val svgElement = svg(
      svgId -> "mySvg",
      svgClass -> "svg-container",
      svgStyle -> "fill: blue;",
      svgWidth -> "100",
      svgHeight -> "100",
      circle(
        cx -> "50",
        cy -> "50",
        r -> "40",
        svgTitle("SVG title element")
      )
    )
    
    // Combined usage
    val combined = div(
      h1("Hello World"),
      svg(
        svgWidth -> "200",
        svgHeight -> "200",
        rect(
          x -> "10",
          y -> "10",
          svgWidth -> "180",
          svgHeight -> "180",
          svgStyle -> "fill: green;"
        )
      )
    )
  }
  
  test("should maintain proper namespaces for attributes") {
    import unifiedAll.*
    
    // HTML style attribute should use XHTML namespace
    val htmlStyle = style
    val htmlAttr = htmlStyle("color: red;")
    htmlAttr match {
      case HtmlAttribute(name, _, ns, _) =>
        assert(name == "style")
        assert(ns == Namespace.xhtml)
    }
    
    // SVG style attribute should use SVG namespace
    val svgStyleAttr = svgStyle
    val svgAttr = svgStyleAttr("fill: blue;")
    svgAttr match {
      case HtmlAttribute(name, _, ns, _) =>
        assert(name == "style")
        assert(ns == Namespace.svg)
    }
  }
  
  test("should handle conflicting tag names correctly") {
    import unifiedAll.*
    
    // HTML title in HtmlTagsExtra
    val htmlTitle = tags_extra.title
    
    // SVG title element
    val svgTitleElement = svgTitle("This is a tooltip")
    
    // They should be different elements with different namespaces
    assert(htmlTitle.name == "title")
    assert(htmlTitle.namespace == Namespace.xhtml)
    
    // SVG title should have SVG namespace
    svgTitleElement match {
      case elem: HtmlElement =>
        assert(elem.name == "title")
        assert(elem.namespace == Namespace.svg)
    }
  }
  
  test("should support mixed HTML and SVG content") {
    import unifiedAll.*
    
    val mixedContent = div(
      h2("Data Visualization"),
      p("Here's a simple chart:"),
      svg(
        svgWidth -> "300",
        svgHeight -> "200",
        svgClass -> "chart",
        g(
          transform -> "translate(50, 50)",
          rect(
            svgWidth -> "200",
            svgHeight -> "100",
            svgStyle -> "fill: steelblue;"
          ),
          text(
            x -> "100",
            y -> "50",
            textAnchor -> "middle",
            svgStyle -> "fill: white; font-size: 20px;",
            "Chart"
          )
        )
      ),
      p("Chart description goes here.")
    )
  }
}