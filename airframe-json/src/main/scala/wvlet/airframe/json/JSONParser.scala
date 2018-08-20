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

import java.util.regex.Pattern

import wvlet.airframe.json.JSON._

import scala.util.Try

/**
  *
  */
object JSONParser {
  def parse(s: JSONSource): JSONValue = {
    val parser = new JSONParser(s)
    JSONScanner.scan(s, parser)
    parser.result
  }
}

private class JSONParser(s: JSONSource) extends JSONEventHandler {
  private var stack = Seq.newBuilder[JSONValue] :: Nil

  def result: JSONValue = {
    val v = stack.head.result()
    if (v.size != 1) {
      throw new InvalidJSONObject(s"contains multiple JSON values: ${s}")
    }
    v.head
  }

  override def startJson(s: JSONSource, start: Int): Unit = {}
  override def endJson(s: JSONSource, start: Int): Unit   = {}
  override def startObject(s: JSONSource, start: Int): Unit = {
    stack = Seq.newBuilder[JSONValue] :: stack
  }
  override def endObject(s: JSONSource, start: Int, end: Int, numElem: Int): Unit = {
    val h = stack.head.result()
    if ((h.size % 2) != 0) {
      throw new InvalidJSONObject(s"json object contains ${h.size} elements")
    }

    val m = Seq.newBuilder[(String, JSONValue)]
    var i = 0
    val N = h.size
    while (i < N) {
      h(i) match {
        case JSONString(s) =>
          m += s -> h(i + 1)
        case _ =>
          throw new InvalidJSONObject(s"json string is expected but found: ${h(i)}")
      }
      i += 2
    }
    stack = stack.tail
    stack.head += JSONObject(m.result())
  }

  override def startArray(s: JSONSource, start: Int): Unit = {
    stack = IndexedSeq.newBuilder[JSONValue] :: stack
  }
  override def endArray(s: JSONSource, start: Int, end: Int, numElem: Int): Unit = {
    val h = stack.head.result()
    stack = stack.tail
    stack.head += JSONArray(h)
  }

  override def nullValue(s: JSONSource, start: Int, end: Int): Unit = {
    stack.head += JSONNull
  }
  override def stringValue(s: JSONSource, start: Int, end: Int): Unit = {
    stack.head += JSONString(s.substring(start, end))
  }

  override def numberValue(s: JSONSource, start: Int, end: Int, dotIndex: Int, expIndex: Int): Unit = {
    val v = s.substring(start, end)
    if (dotIndex >= 0 && expIndex >= 0) {
      stack.head += JSONDouble(v.toDouble)
    } else {
      Try(JSONLong(v.toLong))
        .recover {
          case e: NumberFormatException =>
            JSONDouble(v.toDouble)
        }
        .map(stack.head += _)
    }
  }

  override def booleanValue(s: JSONSource, v: Boolean, start: Int, end: Int): Unit = {
    stack.head += (if (v) JSONTrue else JSONFalse)
  }
}
