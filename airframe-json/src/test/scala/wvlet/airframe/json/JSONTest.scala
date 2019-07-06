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
import wvlet.airframe.AirframeSpec
import wvlet.airframe.json.JSON.{JSONArray, JSONLong, JSONNumber, JSONObject}

/**
  *
  */
class JSONTest extends AirframeSpec {
  "support toJSONValue" in {
    val json: Json = """{"id":1}"""
    json.toJSONValue shouldBe JSON.parse(json)
  }

  "JSONObject.get() and JSONArray.apply()" in {
    val json: Json = """{"user": [{ "id": 1 }, { "id": 2 }]}"""
    val jsonValue  = JSON.parse(json)

    val id = for {
      users <- jsonValue.asInstanceOf[JSONObject].get("user")
      user  <- Some(users.asInstanceOf[JSONArray](0))
      id    <- user.asInstanceOf[JSONObject].get("id")
    } yield id.asInstanceOf[JSONLong].v

    id shouldBe Some(1)
  }

  "JSON DSL" in {
    val json: Json = """{"user": [{ "id": 1 }, { "id": 2 }]}"""
    val jsonValue  = JSON.parse(json)

    val id = (jsonValue \ "user" \ "id")(0).value

    id shouldBe 1
  }
}
