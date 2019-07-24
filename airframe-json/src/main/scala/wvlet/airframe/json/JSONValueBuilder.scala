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

import wvlet.airframe.json.JSON._
import wvlet.log.LogSupport

import scala.collection.mutable.ListBuffer

class JSONValueBuilder extends JSONContext[JSONValue] with LogSupport { self =>

  override def result: JSONValue                           = null
  override def isObjectContext: Boolean                    = false
  override def closeContext(): Unit = {}
  def add(v: JSONValue): Unit                              = {}

  override def singleContext(): JSONContext[JSONValue] =
    new JSONValueBuilder {
      private[this] var holder: JSONValue                      = _
      override def isObjectContext                             = false
      override def closeContext(): Unit = {}
      override def add(v: JSONValue): Unit = {
        holder = v
      }
      override def result: JSONValue = holder
    }

  override def objectContext(): JSONContext[JSONValue] =
    new JSONValueBuilder {
      private[this] var key: String = _
      private[this] val list        = new ListBuffer[(String, JSONValue)]()
      private[this] val value       = JSONObject(list)
      override def closeContext(): Unit = {
        self.add(value)
      }
      override def isObjectContext: Boolean = true
      override def add(v: JSONValue): Unit = {
        if (key == null) {
          key = v.toString
        } else {
          list.append(key -> v)
          key = null
        }
      }
      override def result: JSONValue = value
    }

  override def arrayContext(): JSONContext[JSONValue] =
    new JSONValueBuilder {
      private[this] val list                = new ListBuffer[JSONValue]()
      private[this] val value               = JSONArray(list)
      override def isObjectContext: Boolean = false
      override def closeContext(): Unit = {
        self.add(value)
      }
      override def add(v: JSONValue): Unit = {
        list.append(v)
      }
      override def result: JSONValue = value
    }

  override def addNull(s: JSONSource, start: Int, end: Int): Unit = {
    add(JSONNull)
  }
  override def addString(s: JSONSource, start: Int, end: Int): Unit = {
    add(JSONString(s.substring(start, end)))
  }
  override def addUnescapedString(s: String): Unit = {
    add(JSONString(s))
  }
  override def addNumber(s: String): Unit = {
    val num = try {
      JSONLong(s.toLong)
    } catch {
      case _: Exception => JSONDouble(s.toDouble)
    }
    add(num)
  }

  override def addBoolean(s: JSONSource, v: Boolean, start: Int, end: Int): Unit = {
    val b = if (v) JSONTrue else JSONFalse
    add(b)
  }
}
