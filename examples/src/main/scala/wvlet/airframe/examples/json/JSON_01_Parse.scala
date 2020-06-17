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
package wvlet.airframe.examples.json

/**
  */
object JSON_01_Parse extends App {
  import wvlet.airframe.json.JSON

  val j    = JSON.parse("""{"id":1, "name":"leo"}""")
  val id   = (j / "id").value
  val name = (j / "name").value
}
