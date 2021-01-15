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
package example.nested.v1

import example.nested.v1.MyApi.{HelloRequest, HelloResponse, Message}
import wvlet.airframe.http.RPC

/**
  */
@RPC
trait MyApi {
  def hello(request: HelloRequest): HelloResponse = HelloResponse("hello v1")
  def helloMsg: Seq[Message] = Seq.empty
}

object MyApi {
  case class Message(msg: String)
  case class HelloRequest(name: String)
  case class HelloResponse(message: String)
}
