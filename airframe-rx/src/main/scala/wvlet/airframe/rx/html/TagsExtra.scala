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

/**
  * Contains tags that are rarely used
  */
trait TagsExtra {

  // Defines an abbreviation or an acronym
  def abbr: HtmlElement = tag("abbr")
  // Not supported in HTML5. Use <abbr> instead.
  def acronym: HtmlElement = tag("acronym")
  // Defines contact information for the author/owner of a document
  def address: HtmlElement = tag("address")
  // Not supported in HTML5. Use <embed> or <object> instead.
  def applet: HtmlElement = tag("applet")
  // Defines an article
  def article: HtmlElement = tag("article")
  // Defines content aside from the page content
  def aside: HtmlElement = tag("aside")
  // Defines sound content
  def audio: HtmlElement = tag("audio")

  // Specifies the base URL/target for all relative URLs in a document
  def base: HtmlElement = tag("base")
  // Not supported in HTML5. Use CSS instead.
  def basefont: HtmlElement = tag("basefont")
  // Isolates a part of text that might be formatted in a different direction from other text outside it
  def bdi: HtmlElement = tag("bdi")
  // Overrides the current text direction
  def bdo: HtmlElement = tag("bdo")
  // Not supported in HTML5. Use CSS instead.
  def big: HtmlElement = tag("big")

  // Links the given content with a machine-readable translation
  def data: HtmlElement = tag("data")
  // Specifies a list of pre-defined options for input controls
  def datalist: HtmlElement = tag("datalist")

  // Not supported in HTML5. Use   // instead.
  def dir: HtmlElement = tag("dir")

  // Defines information about the document
  def head: HtmlElement = tag("head")
  // Defines a header for a document or section
  def header: HtmlElement = tag("header")
  // Defines a thematic change in the content

  // Defines marked/highlighted text
  def mark: HtmlElement = tag("mark")

  // Defines the result of a calculation
  def output: HtmlElement = tag("output")

  // Represents the progress of a task
  def progress: HtmlElement = tag("progress")

  // Defines sample output from a computer program
  def samp: HtmlElement = tag("samp")

  // Defines style information for a document
  def style: HtmlElement = tag("style")
  // Defines a date/time
  def time: HtmlElement = tag("time")
  // Defines a title for the document
  def title: HtmlElement = tag("title")
  // Defines text tracks for media elements (<video> and <audio>)
  def track: HtmlElement = tag("track")
  // Not supported in HTML5. Use CSS instead.
  def tt: HtmlElement = tag("tt")

  // Defines a variable
  def _var: HtmlElement = tag("var")
  // Defines a video or movie
  def video: HtmlElement = tag("video")

}
