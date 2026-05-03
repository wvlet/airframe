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

import java.io.ByteArrayInputStream

object InputStreamMessageTest extends AirSpec {

  test("read content bytes") {
    val data = "hello world".getBytes("UTF-8")
    val msg  = new InputStreamMessage(new ByteArrayInputStream(data))
    msg.toContentBytes shouldBe data
  }

  test("read content string") {
    val msg = new InputStreamMessage(new ByteArrayInputStream("test string".getBytes("UTF-8")))
    msg.toContentString shouldBe "test string"
  }

  test("getInputStream returns readable stream") {
    val data = "stream data".getBytes("UTF-8")
    val msg  = new InputStreamMessage(new ByteArrayInputStream(data))
    val is   = msg.getInputStream
    is.readAllBytes() shouldBe data
  }

  test("getInputStream after toContentBytes returns cached data") {
    val data  = "cached".getBytes("UTF-8")
    val msg   = new InputStreamMessage(new ByteArrayInputStream(data))
    val bytes = msg.toContentBytes
    bytes shouldBe data
    // After toContentBytes, getInputStream should return a ByteArrayInputStream over cached data
    val is = msg.getInputStream
    is.readAllBytes() shouldBe data
  }

  test("toContentBytes after getInputStream returns cached data") {
    val data = "reverse order".getBytes("UTF-8")
    val msg  = new InputStreamMessage(new ByteArrayInputStream(data))
    // Consume the stream first
    val is = msg.getInputStream
    is.readAllBytes() shouldBe data
    // toContentBytes should still work via cache
    msg.toContentBytes shouldBe data
  }

  test("isEmpty returns false") {
    val msg = new InputStreamMessage(new ByteArrayInputStream(Array.empty[Byte]))
    msg.isEmpty shouldBe false
  }
}
