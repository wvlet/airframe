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
package wvlet.airframe.rx.html.widget.ui

import org.scalajs.dom
import org.scalajs.dom.html
import wvlet.airframe.rx.html.{Embedded, RxElement}

case class Canvas2D(canvas: html.Canvas, context: dom.CanvasRenderingContext2D) extends RxElement {
  override def render: RxElement = Embedded(canvas)
}

/**
  */
object Canvas {

  def new2DCanvas(width: Int = 100, height: Int = 100): Canvas2D = {
    val canvas = dom.document.createElement("canvas").asInstanceOf[html.Canvas]
    canvas.width = width
    canvas.height = height
    val renderer = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    Canvas2D(canvas, renderer)
  }

  def newCanvas(width: Int = 100, height: Int = 100): Canvas2D = new2DCanvas(width = width, height = height)

}
