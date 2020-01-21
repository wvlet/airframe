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
  *
  */
object tags {

  def div: HtmlElement   = tag("div")
  def img: HtmlElement   = tag("img")
  def table: HtmlElement = tag("table")
  def th: HtmlElement    = tag("th")
  def td: HtmlElement    = tag("td")
  def tr: HtmlElement    = tag("tr")
  def a: HtmlElement     = tag("a")
  def p: HtmlElement     = tag("p")
  def code: HtmlElement  = tag("code")
  def pre: HtmlElement   = tag("pre")
  def svg: HtmlElement   = tag("svg")

  def src: HtmlAttributeOf    = attributeOf("src")
  def href: HtmlAttributeOf   = attributeOf("href")
  def _class: HtmlAttributeOf = attributeOf("class")
  def cls: HtmlAttributeOf    = attributeOf("class")
  def style: HtmlAttributeOf  = attributeOf("style")
  def id: HtmlAttributeOf     = attributeOf("id")
  //def onClick[U](handler: => U) =

}
