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

class HttpRequestAttachmentTest extends AirSpec {

  test("set and get attachments") {
    val request = Http.GET("/test")
    
    request.setAttachment("key1", "value1")
    request.setAttachment("key2", 42)
    request.setAttachment("key3", List(1, 2, 3))
    
    request.getAttachment[String]("key1") shouldBe Some("value1")
    request.getAttachment[Int]("key2") shouldBe Some(42)
    request.getAttachment[List[Int]]("key3") shouldBe Some(List(1, 2, 3))
    request.getAttachment[String]("nonexistent") shouldBe None
  }

  test("check attachment existence") {
    val request = Http.GET("/test")
    
    request.hasAttachment("key1") shouldBe false
    request.setAttachment("key1", "value1")
    request.hasAttachment("key1") shouldBe true
  }

  test("remove attachments") {
    val request = Http.GET("/test")
    
    request.setAttachment("key1", "value1")
    request.setAttachment("key2", "value2")
    
    request.removeAttachment("key1") shouldBe Some("value1")
    request.hasAttachment("key1") shouldBe false
    request.hasAttachment("key2") shouldBe true
  }

  test("clear all attachments") {
    val request = Http.GET("/test")
    
    request.setAttachment("key1", "value1")
    request.setAttachment("key2", "value2")
    request.setAttachment("key3", "value3")
    
    request.clearAttachments()
    
    request.hasAttachment("key1") shouldBe false
    request.hasAttachment("key2") shouldBe false
    request.hasAttachment("key3") shouldBe false
    request.attachment shouldBe Map.empty
  }

  test("get all attachments as map") {
    val request = Http.GET("/test")
    
    request.setAttachment("key1", "value1")
    request.setAttachment("key2", 42)
    
    val attachments = request.attachment
    attachments.size shouldBe 2
    attachments("key1") shouldBe "value1"
    attachments("key2") shouldBe 42
  }

  test("preserve attachments when copying with header") {
    val request = Http.GET("/test")
    request.setAttachment("key1", "value1")
    request.setAttachment("key2", 42)
    
    val modifiedRequest = request.withHeader("X-Custom", "custom-value")
    
    modifiedRequest.getAttachment[String]("key1") shouldBe Some("value1")
    modifiedRequest.getAttachment[Int]("key2") shouldBe Some(42)
    modifiedRequest.getHeader("X-Custom") shouldBe Some("custom-value")
  }

  test("preserve attachments when copying with message") {
    val request = Http.GET("/test")
    request.setAttachment("key1", "value1")
    
    val modifiedRequest = request.withContent("test content")
    
    modifiedRequest.getAttachment[String]("key1") shouldBe Some("value1")
    modifiedRequest.contentString shouldBe "test content"
  }

  test("preserve attachments when changing method") {
    val request = Http.GET("/test")
    request.setAttachment("key1", "value1")
    
    val postRequest = request.withMethod("POST")
    
    postRequest.method shouldBe "POST"
    postRequest.getAttachment[String]("key1") shouldBe Some("value1")
  }

  test("preserve attachments when changing uri") {
    val request = Http.GET("/test")
    request.setAttachment("key1", "value1")
    
    val modifiedRequest = request.withUri("/new-path")
    
    modifiedRequest.uri shouldBe "/new-path"
    modifiedRequest.getAttachment[String]("key1") shouldBe Some("value1")
  }

  test("preserve attachments when setting destination") {
    val request = Http.GET("/test")
    request.setAttachment("key1", "value1")
    
    val dest = ServerAddress("localhost", 8080)
    val modifiedRequest = request.withDest(dest)
    
    modifiedRequest.dest shouldBe Some(dest)
    modifiedRequest.getAttachment[String]("key1") shouldBe Some("value1")
  }

  test("preserve attachments when setting remote address") {
    val request = Http.GET("/test")
    request.setAttachment("key1", "value1")
    
    val remoteAddr = ServerAddress("192.168.1.1", 12345)
    val modifiedRequest = request.withRemoteAddress(remoteAddr)
    
    modifiedRequest.remoteAddress shouldBe Some(remoteAddr)
    modifiedRequest.getAttachment[String]("key1") shouldBe Some("value1")
  }

  test("preserve attachments with filter") {
    val request = Http.GET("/test")
    request.setAttachment("key1", "value1")
    
    val modifiedRequest = request.withFilter(_.withHeader("X-Filter", "applied"))
    
    modifiedRequest.getHeader("X-Filter") shouldBe Some("applied")
    modifiedRequest.getAttachment[String]("key1") shouldBe Some("value1")
  }

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

  test("handle complex attachment types") {
    case class UserContext(userId: String, roles: List[String])
    
    val request = Http.GET("/test")
    val userContext = UserContext("user123", List("admin", "user"))
    
    request.setAttachment("userContext", userContext)
    request.setAttachment("metadata", Map("version" -> "1.0", "source" -> "api"))
    
    request.getAttachment[UserContext]("userContext") shouldBe Some(userContext)
    request.getAttachment[Map[String, String]]("metadata") shouldBe Some(Map("version" -> "1.0", "source" -> "api"))
  }
}