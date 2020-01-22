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
package wvlet.airframe.rx.widget.ui
import wvlet.airframe.rx.html.all._
import wvlet.airframe.rx.html.{HtmlElement, RxComponent, tags}
import wvlet.airframe.rx.widget.ui.Layout.divOf

/**
  * Twitter Bootstrap extensions
  */
package object bootstrap {
  implicit class RichRxComponent(val x: RxComponent) extends AnyVal {
    def withBorder        = x(_class -> "border")
    def withRoundedCorner = x(_class -> "rounded")
//    def withShadow: RxComponent        = x(_class -> "shadow-sm")
//
//    def withOverflowAuto   = x(_class -> "overflow-auto")
//    def withOverflowHidden = x(_class -> "overflow-hidden")
//
//    def withPositionStatic   = x(_class -> "position-static")
//    def withPositionRelative = x(_class -> "position-relative")
//    def withPositionAbsolute = x(_class -> "position-absolute")
//    def withPositionFixed    = x(_class -> "position-fiOxed")
//    def withPositionSticky   = x(_class -> "position-sticky")
//
//    def withFixedTop    = x(_class -> "fixed-top")
//    def withFixedBottom = x(_class -> "fixed-bottom")
//    def withStickyTop   = x(_class -> "sticky-top")
//
//    def withAlertLink = x(_class -> "alert-link")
//
//    def screenReadersOnly = x(_class -> "sr-only")
  }

  def row: HtmlElement   = divOf("row")
  def col: HtmlElement   = divOf("col")
  def col1: HtmlElement  = divOf("col-1")
  def col2: HtmlElement  = divOf("col-2")
  def col3: HtmlElement  = divOf("col-3")
  def col4: HtmlElement  = divOf("col-4")
  def col5: HtmlElement  = divOf("col-5")
  def col6: HtmlElement  = divOf("col-6")
  def col7: HtmlElement  = divOf("col-7")
  def col8: HtmlElement  = divOf("col-8")
  def col9: HtmlElement  = divOf("col-9")
  def col10: HtmlElement = divOf("col-10")
  def col11: HtmlElement = divOf("col-11")
  def col12: HtmlElement = divOf("col-12")
  def colSm: HtmlElement = divOf("col-sm")

  def container: HtmlElement      = divOf("container")
  def containerFluid: HtmlElement = divOf("container-fluid")

  def flexbox: HtmlElement       = divOf("d-flex")
  def inlineFlexbox: HtmlElement = divOf("d-inline-flex")

  def figure: HtmlElement = tags.figure(cls -> "figure")
}
