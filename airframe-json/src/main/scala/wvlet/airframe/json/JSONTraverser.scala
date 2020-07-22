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

trait JSONVisitor {
  def visitObject(o: JSONObject): Unit = {}
  def leaveObject(o: JSONObject): Unit = {}
  def visitKeyValue(k: String, v: JSONValue): Unit = {}
  def leaveKeyValue(k: String, v: JSONValue): Unit = {}
  def visitArray(a: JSONArray): Unit = {}
  def leaveArray(a: JSONArray): Unit = {}
  def visitString(v: JSONString): Unit = {}
  def visitNumber(n: JSONNumber): Unit = {}
  def visitBoolean(n: JSONBoolean): Unit = {}
  def visitNull: Unit = {}
}

/**
  */
object JSONTraverser {
  def traverse(json: String, visitor: JSONVisitor): Unit = {
    traverse(JSON.parse(json), visitor)
  }

  def traverse(json: JSONValue, visitor: JSONVisitor): Unit = {
    json match {
      case o: JSONObject =>
        visitor.visitObject(o)
        for ((jk, jv) <- o.v) {
          visitor.visitKeyValue(jk, jv)
          traverse(jv, visitor)
          visitor.leaveKeyValue(jk, jv)
        }
        visitor.leaveObject(o)
      case a: JSONArray =>
        visitor.visitArray(a)
        a.v.foreach(traverse(_, visitor))
        visitor.leaveArray(a)
      case v: JSONString =>
        visitor.visitString(v)
      case v: JSONNumber =>
        visitor.visitNumber(v)
      case v: JSONBoolean =>
        visitor.visitBoolean(v)
      case JSONNull =>
        visitor.visitNull
    }
  }
}
