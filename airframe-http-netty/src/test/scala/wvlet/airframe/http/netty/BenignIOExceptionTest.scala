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
package wvlet.airframe.http.netty

import wvlet.airspec.AirSpec

class BenignIOExceptionTest extends AirSpec {

  test("PrematureChannelClosureException is benign") {
    val ex =
      new io.netty.handler.codec.PrematureChannelClosureException("Channel closed while still aggregating message")
    NettyRequestHandler.isBenignIOException(ex) shouldBe true
  }

  test("ClosedChannelException is benign") {
    val ex = new java.nio.channels.ClosedChannelException()
    NettyRequestHandler.isBenignIOException(ex) shouldBe true
  }

  test("IOException with Connection reset is benign") {
    val ex = new java.io.IOException("Connection reset")
    NettyRequestHandler.isBenignIOException(ex) shouldBe true
  }

  test("IOException with Broken pipe is benign") {
    val ex = new java.io.IOException("Broken pipe")
    NettyRequestHandler.isBenignIOException(ex) shouldBe true
  }

  test("wrapped PrematureChannelClosureException is benign") {
    val cause   = new io.netty.handler.codec.PrematureChannelClosureException("Channel closed")
    val wrapper = new RuntimeException("wrapper", cause)
    NettyRequestHandler.isBenignIOException(wrapper) shouldBe true
  }

  test("RuntimeException with unrelated message is not benign") {
    val ex = new RuntimeException("some other error")
    NettyRequestHandler.isBenignIOException(ex) shouldBe false
  }

  test("null cause is not benign") {
    NettyRequestHandler.isBenignIOException(null) shouldBe false
  }
}
