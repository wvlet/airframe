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
t * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.api

import example.api.HelloApi.HelloResponse
import wvlet.airframe.http.RPC

object HelloApi {
  case class HelloResponse(message: String)
}

@RPC
trait HelloApi {
  def hello(message: String): HelloResponse
}
