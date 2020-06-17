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
package wvlet.airframe.http.rx.html

/**
  */
trait Tags {

  // Defines a hyperlink
  lazy val a: HtmlElement = tag("a")
  // Defines an area inside an image-map
  lazy val area: HtmlElement = tag("area")
  // Defines bold text
  lazy val b: HtmlElement = tag("b")
  // Defines a section that is quoted from another source
  lazy val blockquote: HtmlElement = tag("blockquote")
  // Defines the document's body
  lazy val body: HtmlElement = tag("body")
  // Defines a single line break
  lazy val br: HtmlElement = tag("br")
  // Defines a clickable button
  lazy val button: HtmlElement = tag("button")
  // Used to draw graphics, on the fly, via scripting (usually JavaScript)
  lazy val canvas: HtmlElement = tag("canvas")
  // Defines a table caption
  lazy val caption: HtmlElement = tag("caption")
  // Not supported in HTML5. Use CSS instead.
  lazy val center: HtmlElement = tag("center")
  // Defines the title of a work
  lazy val cite: HtmlElement = tag("cite")
  // Defines a piece of computer code
  lazy val code: HtmlElement = tag("code")
  lazy val dd: HtmlElement   = tag("dd")
  // Defines text that has been deleted from a document
  lazy val del: HtmlElement = tag("del")
  // Defines additional details that the user can view or hide
  lazy val details: HtmlElement = tag("details")
  // Represents the lazy valining instance of a term
  lazy val dfn: HtmlElement = tag("dfn")
  // Defines a dialog box or window
  lazy val dialog: HtmlElement = tag("dialog")
  // Defines a section in a document
  lazy val div: HtmlElement = tag("div")
  // Defines a description list
  lazy val dl: HtmlElement = tag("dl")
  // Defines a term/name in a description list
  lazy val dt: HtmlElement = tag("dt")
  // Defines emphasized text
  lazy val em: HtmlElement = tag("em")
  // Defines a container for an external (non-HTML) application
  lazy val embed: HtmlElement = tag("embed")
  // Groups related elements in a form
  lazy val fieldset: HtmlElement = tag("fieldset")
  // Defines a caption for a <figure> element
  lazy val figcaption: HtmlElement = tag("figcaption")
  // Specifies self-contained content
  lazy val figure: HtmlElement = tag("figure")
  // Not supported in HTML5. Use CSS instead.
  lazy val font: HtmlElement = tag("font")
  // Defines a footer for a document or section
  lazy val footer: HtmlElement = tag("footer")
  // Defines an HTML form for user input
  lazy val form: HtmlElement = tag("form")
  // Not supported in HTML5.
  lazy val frame: HtmlElement = tag("frame")
  // Not supported in HTML5.
  lazy val frameset: HtmlElement = tag("frameset")
  // Defines HTML headings
  lazy val h1: HtmlElement = tag("h1")
  lazy val h2: HtmlElement = tag("h2")
  lazy val h3: HtmlElement = tag("h3")
  lazy val h4: HtmlElement = tag("h4")
  lazy val h5: HtmlElement = tag("h5")
  lazy val h6: HtmlElement = tag("h6")

  lazy val hr: HtmlElement = tag("hr")
  // Defines the root of an HTML document
  lazy val html: HtmlElement = tag("html")
  // Defines a part of text in an alternate voice or mood
  lazy val i: HtmlElement = tag("i")
  // Defines an inline frame
  lazy val iframe: HtmlElement = tag("iframe")
  // Defines an image
  lazy val img: HtmlElement = tag("img")
  // Defines an input control
  lazy val input: HtmlElement = tag("input")
  // Defines a text that has been inserted into a document
  lazy val ins: HtmlElement = tag("ins")
  // Defines keyboard input
  lazy val kbd: HtmlElement = tag("kbd")
  // Defines a label for an <input> element
  lazy val label: HtmlElement = tag("label")
  // Defines a caption for a <fieldset> element
  lazy val legend: HtmlElement = tag("legend")
  // Defines a list item
  lazy val li: HtmlElement = tag("li")
  // Defines the relationship between a document and an external resource (most used to link to style sheets)
  lazy val link: HtmlElement = tag("link")
  // Specifies the main content of a document
  lazy val main: HtmlElement = tag("main")
  // Defines a client-side image-map
  lazy val map: HtmlElement = tag("map")
  // Defines metadata about an HTML document
  lazy val meta: HtmlElement = tag("meta")
  // Defines a scalar measurement within a known range (a gauge)
  lazy val meter: HtmlElement = tag("meter")
  // Defines navigation links
  lazy val nav: HtmlElement = tag("nav")
  // Not supported in HTML5.
  lazy val noframes: HtmlElement = tag("noframes")
  // Defines an alternate content for users that do not support client-side scripts
  lazy val noscript: HtmlElement = tag("noscript")
  // Defines an embedded object
  lazy val _object: HtmlElement = tag("object")
  // Defines an ordered list
  lazy val ol: HtmlElement = tag("ol")
  // Defines a group of related options in a drop-down list
  lazy val optgroup: HtmlElement = tag("optgroup")
  // Defines an option in a drop-down list
  lazy val option: HtmlElement = tag("option")
  // Defines a paragraph
  lazy val p: HtmlElement = tag("p")
  // Defines a parameter for an object
  lazy val param: HtmlElement = tag("param")
  // Defines a container for multiple image resources
  lazy val picture: HtmlElement = tag("picture")
  // Defines preformatted text
  lazy val pre: HtmlElement = tag("pre")
  // Defines a short quotation
  lazy val q: HtmlElement = tag("q")
  // Defines what to show in browsers that do not support ruby annotations
  lazy val rp: HtmlElement = tag("rp")
  // Defines an explanation/pronunciation of characters (for East Asian typography)
  lazy val rt: HtmlElement = tag("rt")
  // Defines text that is no longer correct
  lazy val s: HtmlElement = tag("s")
  // Defines a client-side script
  lazy val script: HtmlElement = tag("script")
  // Defines a section in a document
  lazy val section: HtmlElement = tag("section")
  // Defines a drop-down list
  lazy val select: HtmlElement = tag("select")
  // Defines smaller text
  lazy val small: HtmlElement = tag("small")
  // Defines multiple media resources for media elements (<video> and <audio>)
  lazy val source: HtmlElement = tag("source")
  // Defines a section in a document
  lazy val span: HtmlElement = tag("span")
  // Not supported in HTML5. Use <del> or <s> instead.
  lazy val strike: HtmlElement = tag("strike")
  // Defines important text
  lazy val strong: HtmlElement = tag("strong")
  // Defines subscripted text
  lazy val sub: HtmlElement = tag("sub")
  // Defines a visible heading for a <details> element
  lazy val summary: HtmlElement = tag("summary")
  // Defines superscripted text
  lazy val sup: HtmlElement = tag("sup")
  // Defines a container for SVG graphics
  lazy val svg: HtmlElement = tag("svg")
  // Defines a table
  lazy val table: HtmlElement = tag("table")
  // Groups the body content in a table
  lazy val tbody: HtmlElement = tag("tbody")
  // Defines a cell in a table
  lazy val td: HtmlElement = tag("td")
  // Defines a template
  lazy val template: HtmlElement = tag("template")
  // Defines a multiline input control (text area)
  lazy val textarea: HtmlElement = tag("textarea")
  // Groups the footer content in a table
  lazy val tfoot: HtmlElement = tag("tfoot")
  // Defines a header cell in a table
  lazy val th: HtmlElement = tag("th")
  // Groups the header content in a table
  lazy val thead: HtmlElement = tag("thead")
  // Defines a row in a table
  lazy val tr: HtmlElement = tag("tr")
  // Defines text that should be stylistically different from normal text
  lazy val u: HtmlElement = tag("u")
  // Defines an unordered list
  lazy val ul: HtmlElement = tag("ul")
  // Defines a possible line-break
  lazy val wbr: HtmlElement = tag("wbr")

}
