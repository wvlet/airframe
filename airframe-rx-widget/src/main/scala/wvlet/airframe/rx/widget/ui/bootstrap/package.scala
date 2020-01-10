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
import wvlet.airframe.rx.widget.RxComponentBuilder
import wvlet.airframe.rx.widget.ui.Layout.divOf

/**
  * Twitter Bootstrap extensions
  */
package object bootstrap {
  implicit class RichRxComponentBuilder(val x: RxComponentBuilder) extends AnyVal {
    def withBorder: RxComponentBuilder        = x.withClasses("border")
    def withRoundedCorner: RxComponentBuilder = x.withClasses("rounded")
    def withShadow: RxComponentBuilder        = x.withClasses("shadow-sm")

    def withOverflowAuto   = x.withClasses("overflow-auto")
    def withOverflowHidden = x.withClasses("overflow-hidden")

    def withPositionStatic   = x.withClasses("position-static")
    def withPositionRelative = x.withClasses("position-relative")
    def withPositionAbsolute = x.withClasses("position-absolute")
    def withPositionFixed    = x.withClasses("position-fixed")
    def withPositionSticky   = x.withClasses("position-sticky")

    def withFixedTop    = x.withClasses("fixed-top")
    def withFixedBottom = x.withClasses("fixed-bottom")
    def withStickyTop   = x.withClasses("sticky-top")

    def withAlertLink = x.withClasses("alert-link")
  }

  def row: RxComponentBuilder   = divOf("row")
  def col: RxComponentBuilder   = divOf("col")
  def col1: RxComponentBuilder  = divOf("col-1")
  def col2: RxComponentBuilder  = divOf("col-2")
  def col3: RxComponentBuilder  = divOf("col-3")
  def col4: RxComponentBuilder  = divOf("col-4")
  def col5: RxComponentBuilder  = divOf("col-5")
  def col6: RxComponentBuilder  = divOf("col-6")
  def col7: RxComponentBuilder  = divOf("col-7")
  def col8: RxComponentBuilder  = divOf("col-8")
  def col9: RxComponentBuilder  = divOf("col-9")
  def col10: RxComponentBuilder = divOf("col-10")
  def col11: RxComponentBuilder = divOf("col-11")
  def col12: RxComponentBuilder = divOf("col-12")
  def colSm: RxComponentBuilder = divOf("col-sm")

  def container: RxComponentBuilder      = divOf("container")
  def containerFluid: RxComponentBuilder = divOf("container-fluid")

  def flexbox: RxComponentBuilder       = divOf("d-flex")
  def inlineFlexbox: RxComponentBuilder = divOf("d-inline-flex")

  def figure: RxComponentBuilder = RxComponentBuilder(tag = "figure", primaryClass = "figure")
}
