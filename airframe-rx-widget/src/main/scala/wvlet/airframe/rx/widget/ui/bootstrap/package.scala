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
import wvlet.airframe.rx.widget.RxComponent
import wvlet.airframe.rx.widget.ui.Layout.divOf

/**
  * Twitter Bootstrap extensions
  */
package object bootstrap {
  implicit class RichRxComponent(val x: RxComponent) extends AnyVal {
    def withBorder: RxComponent        = x.addClass("border")
    def withRoundedCorner: RxComponent = x.addClass("rounded")
    def withShadow: RxComponent        = x.addClass("shadow-sm")

    def withOverflowAuto   = x.addClass("overflow-auto")
    def withOverflowHidden = x.addClass("overflow-hidden")

    def withPositionStatic   = x.addClass("position-static")
    def withPositionRelative = x.addClass("position-relative")
    def withPositionAbsolute = x.addClass("position-absolute")
    def withPositionFixed    = x.addClass("position-fixed")
    def withPositionSticky   = x.addClass("position-sticky")

    def withFixedTop    = x.addClass("fixed-top")
    def withFixedBottom = x.addClass("fixed-bottom")
    def withStickyTop   = x.addClass("sticky-top")

    def withAlertLink = x.addClass("alert-link")

    def screenReadersOnly = x.addClass("sr-only")
  }

  def row: RxComponent   = divOf("row")
  def col: RxComponent   = divOf("col")
  def col1: RxComponent  = divOf("col-1")
  def col2: RxComponent  = divOf("col-2")
  def col3: RxComponent  = divOf("col-3")
  def col4: RxComponent  = divOf("col-4")
  def col5: RxComponent  = divOf("col-5")
  def col6: RxComponent  = divOf("col-6")
  def col7: RxComponent  = divOf("col-7")
  def col8: RxComponent  = divOf("col-8")
  def col9: RxComponent  = divOf("col-9")
  def col10: RxComponent = divOf("col-10")
  def col11: RxComponent = divOf("col-11")
  def col12: RxComponent = divOf("col-12")
  def colSm: RxComponent = divOf("col-sm")

  def container: RxComponent      = divOf("container")
  def containerFluid: RxComponent = divOf("container-fluid")

  def flexbox: RxComponent       = divOf("d-flex")
  def inlineFlexbox: RxComponent = divOf("d-inline-flex")

  def figure: RxComponent = RxComponent { content =>
    <figure class="figure">{content}</figure>
  }
}
