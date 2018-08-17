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
package wvlet.airframe.json

import wvlet.log.LogSupport

trait JSONEventHandler {
  def startJson(s: JSONSource, start: Int): Unit
  def endJson(s: JSONSource, start: Int): Unit
  def startObject(s: JSONSource, start: Int): Unit
  def endObject(s: JSONSource, start: Int, end: Int, numElem: Int)
  def startArray(s: JSONSource, start: Int): Unit
  def endArray(s: JSONSource, start: Int, end: Int, numElem: Int)

  def nullValue(s: JSONSource, start: Int, end: Int): Unit
  def stringValue(s: JSONSource, start: Int, end: Int): Unit
  def numberValue(s: JSONSource, start: Int, end: Int): Unit
  def booleanValue(s: JSONSource, v: Boolean, start: Int, end: Int)
}
