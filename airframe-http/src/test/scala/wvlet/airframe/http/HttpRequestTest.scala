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

class HttpRequestTest extends AirSpec {

  test("accept application/msgpack") {
    val r = Http.POST("/").withAcceptMsgPack
    r.acceptsMsgPack shouldBe true
    r.acceptsJson shouldBe false
  }

  test("accept application/x-msgpack") {
    val r = Http.POST("/").withAccept("application/x-msgpack")
    r.acceptsMsgPack shouldBe true
    r.acceptsJson shouldBe false
  }

  test("accept application/json;encoding=utf-8") {
    val r = Http.POST("/").withAcceptJson
    r.acceptsJson shouldBe true
    r.acceptsMsgPack shouldBe false
  }

  test("accept application/json") {
    val r = Http.POST("/").withAccept("application/json")
    r.acceptsJson shouldBe true
    r.acceptsMsgPack shouldBe false
  }

  test("content-type application/msgpack") {
    val r = Http.POST("/").withContentTypeMsgPack
    r.isContentTypeMsgPack shouldBe true
    r.isContentTypeJson shouldBe false
  }

  test("content-type application/x-msgpack") {
    val r = Http.POST("/").withContentType("application/x-msgpack")
    r.isContentTypeMsgPack shouldBe true
    r.isContentTypeJson shouldBe false
  }

  test("content-type application/json;encoding=utf-8") {
    val r = Http.POST("/").withContentTypeJson
    r.isContentTypeJson shouldBe true
    r.isContentTypeMsgPack shouldBe false
  }

  test("content-type application/json") {
    val r = Http.POST("/").withContentType("application/json")
    r.isContentTypeJson shouldBe true
    r.isContentTypeMsgPack shouldBe false
  }

}
