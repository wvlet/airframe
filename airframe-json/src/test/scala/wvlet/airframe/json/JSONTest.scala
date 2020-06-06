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
import wvlet.airframe.json.JSON.{JSONArray, JSONLong, JSONNumber, JSONObject}
import wvlet.airspec.AirSpec

/**
  *
  */
class JSONTest extends AirSpec {
  scalaJsSupport

  def `support toJSONValue`: Unit = {
    val json: Json = """{"id":1}"""
    json.toJSONValue shouldBe JSON.parse(json)
  }

  def `JSONObject.get() and JSONArray.apply()` : Unit = {
    val json: Json = """{"user": [{ "id": 1 }, { "id": 2 }]}"""
    val jsonValue  = JSON.parse(json)

    val id = for {
      users <- jsonValue.asInstanceOf[JSONObject].get("user")
      user  <- Some(users.asInstanceOf[JSONArray](0))
      id    <- user.asInstanceOf[JSONObject].get("id")
    } yield id.asInstanceOf[JSONLong].v

    id shouldBe Some(1)
  }

  def `JSON DSL`: Unit = {
    val json: Json = """{"user": [{ "id": 1, "name": "a" }, { "id": 2, "name": "b" }]}"""
    val jsonValue  = JSON.parse(json)

    val ids = (jsonValue / "user" / "id").values
    ids shouldBe Seq(1, 2)

    val id1 = (jsonValue / "user" / "id")(0).value
    id1 shouldBe 1

    val id2 = jsonValue("user")(1)("id").toLongValue
    id2 shouldBe 2

    val name1 = jsonValue("user")(0)("name").toStringValue
    name1 shouldBe "a"

    val name2 = (jsonValue / "user" / "name")(1).value
    name2 shouldBe "b"

    val users = (jsonValue / "user").value
    users shouldBe Seq(Map("id" -> 1, "name" -> "a"), Map("id" -> 2, "name" -> "b"))
  }
}
