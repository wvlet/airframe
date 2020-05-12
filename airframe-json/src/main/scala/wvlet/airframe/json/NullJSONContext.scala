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
class NullJSONContext(isObject: Boolean) extends JSONContext[Unit] with LogSupport {
  override def isObjectContext: Boolean                                    = isObject
  override def objectContext(s: JSONSource, start: Int): JSONContext[Unit] = new NullJSONContext(true)
  override def arrayContext(s: JSONSource, start: Int): JSONContext[Unit]  = new NullJSONContext(false)
  override def closeContext(s: JSONSource, end: Int): Unit = {}

  override def add(v: Unit): Unit = {}
  override def singleContext(s: JSONSource, start: Int): JSONContext[Unit] = new NullJSONContext(false)
  override def result: Unit = {}
  override def addNull(s: JSONSource, start: Int, end: Int): Unit = {}
  override def addString(s: JSONSource, start: Int, end: Int): Unit = {}
  override def addUnescapedString(s: String): Unit = {}
  override def addNumber(s: JSONSource, start: Int, end: Int, dotIndex: Int, expIndex: Int): Unit = {}
  override def addBoolean(s: JSONSource, v: Boolean, start: Int, end: Int): Unit = {}
}
