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
import wvlet.airframe.rx.html.{RxComponent, RxElement, tags}
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

  def row: RxElement   = divOf("row")
  def col: RxElement   = divOf("col")
  def col1: RxElement  = divOf("col-1")
  def col2: RxElement  = divOf("col-2")
  def col3: RxElement  = divOf("col-3")
  def col4: RxElement  = divOf("col-4")
  def col5: RxElement  = divOf("col-5")
  def col6: RxElement  = divOf("col-6")
  def col7: RxElement  = divOf("col-7")
  def col8: RxElement  = divOf("col-8")
  def col9: RxElement  = divOf("col-9")
  def col10: RxElement = divOf("col-10")
  def col11: RxElement = divOf("col-11")
  def col12: RxElement = divOf("col-12")
  def colSm: RxElement = divOf("col-sm")

  def container: RxElement      = divOf("container")
  def containerFluid: RxElement = divOf("container-fluid")

  def flexbox: RxElement       = divOf("d-flex")
  def inlineFlexbox: RxElement = divOf("d-inline-flex")

  def figure: RxElement = tags.figure(cls -> "figure")
}
