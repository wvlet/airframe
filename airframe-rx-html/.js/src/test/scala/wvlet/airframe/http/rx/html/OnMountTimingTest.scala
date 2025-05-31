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

import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import wvlet.airframe.rx.{Cancelable, Rx}
import wvlet.airframe.rx.html.{DOMRenderer, RxElement}
import wvlet.airframe.rx.html.all.*
import java.util.UUID
import wvlet.airspec.AirSpec

class OnMountTimingTest extends AirSpec {

  class HoverableTextLabel(txt: RxElement, hoverMessage: String) extends RxElement {
    val elementId = UUID.randomUUID().toString
    var elementFoundInOnMount = false

    override def onMount(node: Any): Unit = {
      // Try to find the element by ID in the document
      Option(org.scalajs.dom.document.getElementById(elementId)) match {
        case Some(el: HTMLElement) =>
          elementFoundInOnMount = true
          debug(s"Found element with id $elementId in onMount")
        case None =>
          debug(s"Element with id $elementId NOT found in onMount")
      }
    }

    override def render: RxElement = span(
      id                   -> elementId,
      data("bs-toggle")    -> "tooltip", 
      data("bs-placement") -> "top",
      data("bs-title")     -> hoverMessage,
      txt
    )
  }

  private def render(v: Any): (HTMLElement, Cancelable) = {
    val (n, c) = v match {
      case rx: Rx[RxElement] @unchecked =>
        DOMRenderer.createNode(div(rx))
      case other: RxElement =>
        DOMRenderer.createNode(other)
    }
    (n.asInstanceOf[HTMLElement], c)
  }

  test("onMount should be able to find DOM element in nested sequence") {
    val hoverableLabel = new HoverableTextLabel(span("hello"), "mouseover message")
    
    val mainDiv = div(
      Seq[RxElement](
        hoverableLabel
      )
    )
    
    // Render and mount to document
    val (node, cancelable) = render(mainDiv)
    dom.document.body.appendChild(node)
    
    try {
      // The element should be found during onMount
      hoverableLabel.elementFoundInOnMount shouldBe true
      
      // Verify that the element actually exists in the document
      val elementExistsNow = Option(org.scalajs.dom.document.getElementById(hoverableLabel.elementId)).isDefined
      elementExistsNow shouldBe true
      
    } finally {
      cancelable.cancel
      dom.document.body.removeChild(node)
    }
  }

  test("onMount should work with deeply nested elements") {
    val deeplyNestedLabel = new HoverableTextLabel(span("deeply nested"), "tooltip")
    
    val deepNesting = div(
      div(
        div(
          Seq[RxElement](
            deeplyNestedLabel
          )
        )
      )
    )
    
    val (node, cancelable) = render(deepNesting)
    dom.document.body.appendChild(node)
    
    try {
      // The element should be found during onMount even in deep nesting
      deeplyNestedLabel.elementFoundInOnMount shouldBe true
      
      // Verify that the element actually exists in the document
      val elementExistsNow = Option(org.scalajs.dom.document.getElementById(deeplyNestedLabel.elementId)).isDefined
      elementExistsNow shouldBe true
      
    } finally {
      cancelable.cancel
      dom.document.body.removeChild(node)
    }
  }
}