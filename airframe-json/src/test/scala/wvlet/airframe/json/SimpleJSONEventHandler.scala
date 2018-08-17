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

/**
  *
  */
object SimpleJSONEventHandler extends JSONEventHandler with LogSupport {
  def startObject(s: JSONSource, start: Int): Unit = {
    debug(s"start obj: ${start}")
  }
  def endObject(s: JSONSource, start: Int, end: Int, numElem: Int): Unit = {
    debug(s"end obj: [${start},${end}),  num elems:${numElem}")
  }
  def startArray(s: JSONSource, start: Int): Unit = {
    debug(s"start array: ${start}")
  }
  def endArray(s: JSONSource, start: Int, end: Int, numElem: Int): Unit = {
    debug(s"end array: [${start},${end}), num elems:${numElem}")
  }
  def stringValue(s: JSONSource, start: Int, end: Int): Unit = {
    debug(s"string value: [${start},${end}) ${s.substring(start, end)}")
  }
  def numberValue(s: JSONSource, start: Int, end: Int): Unit = {
    debug(s"number value: [${start}, ${end}) ${s.substring(start, end)}")
  }
  def booleanValue(s: JSONSource, v: Boolean, start: Int, end: Int): Unit = {
    debug(s"boolean value: [${start}, ${end}) ${s.substring(start, end)}")
  }
  def nullValue(s: JSONSource, start: Int, end: Int): Unit = {
    debug(s"null value: [${start}, ${end}) ${s.substring(start, end)}")
  }
}
