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

import scala.util.Try

class JSONValueBuilder extends JSONContext[JSONValue] with LogSupport { self =>

  override def inObjectContext: Boolean                    = ???
  override def closeContext(s: JSONSource, end: Int): Unit = ???
  override def add(v: JSONValue): Unit                     = ???
  override def result: JSONValue                           = ???

  protected val parent: Option[JSONValueBuilder] = None

  override def singleContext(s: JSONSource, start: Int): JSONValueBuilder = new JSONValueBuilder {
    override protected val parent: Option[JSONValueBuilder]  = Some(self)
    private var holder: JSONValue                            = _
    override def inObjectContext                             = false
    override def closeContext(s: JSONSource, end: Int): Unit = {}
    override def add(v: JSONValue): Unit = {
      holder = v
    }
    override def result: JSONValue = holder
  }

  override def objectContext(s: JSONSource, start: Int): JSONValueBuilder = new JSONValueBuilder {
    override protected val parent: Option[JSONValueBuilder] = Some(self)
    private var key: String                                 = null
    private val list                                        = Seq.newBuilder[(String, JSONValue)]
    override def closeContext(s: JSONSource, end: Int): Unit = {
      parent.map(p => p.add(result))
    }
    override def inObjectContext: Boolean = true
    override def add(v: JSONValue): Unit = {
      if (key == null) {
        key = v.toString
      } else {
        list += key -> v
        key = null
      }
    }
    override def result: JSONValue = {
      JSONObject(list.result())
    }
  }

  override def arrayContext(s: JSONSource, start: Int): JSONValueBuilder = new JSONValueBuilder {
    override protected val parent: Option[JSONValueBuilder] = Some(self)
    private val list                                        = IndexedSeq.newBuilder[JSONValue]
    override def inObjectContext: Boolean                   = false
    override def closeContext(s: JSONSource, end: Int): Unit = {
      parent.map(p => p.add(result))
    }
    override def add(v: JSONValue): Unit = {
      list += v
    }
    override def result: JSONValue = {
      JSONArray(list.result())
    }
  }

  override def nullValue(s: JSONSource, start: Int, end: Int): JSONValue   = JSONNull
  override def stringValue(s: JSONSource, start: Int, end: Int): JSONValue = JSONString(s.substring(start, end))
  override def numberValue(s: JSONSource, start: Int, end: Int, dotIndex: Int, expIndex: Int): JSONValue = {
    val v = s.substring(start, end)
    val num: JSONNumber = if (dotIndex >= 0 || expIndex >= 0) {
      JSONDouble(v.toDouble)
    } else {
      Try(JSONLong(v.toLong)).recover {
        case e: NumberFormatException =>
          JSONDouble(v.toDouble)
      }.get
    }
    num
  }

  override def booleanValue(s: JSONSource, v: Boolean, start: Int, end: Int): JSONValue = {
    if (v) JSONTrue else JSONFalse
  }
}
