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
package wvlet.airframe
import wvlet.airframe.json.JSON.{
  JSONArray,
  JSONBoolean,
  JSONDouble,
  JSONLong,
  JSONNull,
  JSONObject,
  JSONString,
  JSONValue
}

/**
  *
  */
package object json {
  // Alias to encode msgpack into JSON strings with airframe-codec
  type Json = String

  implicit class RichJson(val json: Json) extends AnyVal {
    def toJSONValue: JSONValue = JSON.parseAny(json)
  }

  implicit class JSONValueOps(val jsonValue: JSONValue) extends AnyVal {
    def /(name: String): Seq[JSONValue] = {
      jsonValue match {
        case jsonObject: JSONObject =>
          jsonObject.v.collect {
            case (key, value) if key == name =>
              value
          }
        case jsonArray: JSONArray =>
          jsonArray.v.flatMap {
            case value =>
              value / name
          }
        case _ => Nil
      }
    }

    def value: Any = {
      jsonValue match {
        case JSONNull       => null
        case JSONDouble(x)  => x
        case JSONLong(x)    => x
        case JSONString(x)  => x
        case JSONBoolean(x) => x
        case JSONArray(x)   => x.map(_.value)
        case JSONObject(x)  => x.map(x => (x._1, x._2.value)).toMap
      }
    }
  }

  implicit class JSONValueSeqOps(val jsonValues: Seq[JSONValue]) extends AnyVal {
    def /(name: String): Seq[JSONValue] = {
      jsonValues.flatMap { jsonValue => jsonValue / name }
    }

    def values: Seq[Any] = {
      jsonValues.map { jsonValue => jsonValue.value }
    }

    def value: Any = {
      jsonValues.head.value
    }
  }
}
