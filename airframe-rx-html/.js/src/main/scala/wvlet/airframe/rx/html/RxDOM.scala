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

import org.scalajs.dom
import org.scalajs.dom.EventTarget

/**
  * A collection of helper methods for managing DOM elements
  */
object RxDOM {

  /**
    * A helper method to cast get an HTMLElement at ease
    * @param id
    *   dom node id
    * @return
    */
  def getHTMLElementById(id: String): Option[dom.HTMLElement] = {
    Option(dom.document.getElementById(id)).collect { case el: dom.HTMLElement =>
      el
    }
  }

  /**
    * A helper method to handle Event objects with pattern matching
    * @param e
    * @param f
    */
  def handleEvent(e: EventTarget)(f: PartialFunction[EventTarget, Unit]): Unit = {
    if f.isDefinedAt(e) then {
      f(e)
    }
  }

}
