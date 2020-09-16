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
package example.grpc
import wvlet.airframe.http.RPC
import wvlet.airframe.rx.{Rx, RxStream}

/**
  */
@RPC
trait Streaming {
  def serverStreaming(name: String): RxStream[String] = {
    Rx.sequence("Hello", "See You").map(x => s"${x} ${name}!")
  }
  def clientStreaming(input: RxStream[String]): String = {
    input.toSeq.mkString(", ")
  }
  def bidiStreaming(input: RxStream[String]): RxStream[String] = {
    input.map(x => s"Hello ${x}!")
  }
}
