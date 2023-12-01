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
  * A trait for adding different methods between Scala JVM and Scala.js
  */
trait HtmlNodeBase {

  /**
    * (Scala.js only) Render this element to the DOM node of the given ID. If the corresponding DOM node is not found,
    * this method will create a new DOM node.
    *
    * @param nodeId
    * @return
    *   A cancelable RxDOMNode
    */
  def renderTo(nodeId: String): Unit = {
    // Adding this method to Scala JVM as IntelliJ may not recognize this method
    ???
  }
}
