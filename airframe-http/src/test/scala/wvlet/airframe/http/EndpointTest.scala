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
package wvlet.airframe.http
import wvlet.airspec.AirSpec

/**
  */
object EndpointTest extends AirSpec {

  class MyApp {
    @Endpoint(method = HttpMethod.GET, path = "/v1/info")
    def hello: String = {
      "hello"
    }
  }

  test("test endpoint compilation") {
    // Only need to check HTTP interface can be compiled both in JVM and Scala.js
  }
}
