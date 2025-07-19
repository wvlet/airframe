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

class HttpRequestAttachmentJVMTest extends AirSpec {

  test("support concurrent access") {
    val request = Http.GET("/test")

    // Simulate concurrent access
    val threads = (1 to 10).map { i =>
      new Thread(() => {
        request.setAttachment(s"key$i", s"value$i")
      })
    }

    threads.foreach(_.start())
    threads.foreach(_.join())

    // Verify all attachments were set
    request.attachment.size shouldBe 10
    (1 to 10).foreach { i =>
      request.getAttachment[String](s"key$i") shouldBe Some(s"value$i")
    }
  }
}