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
package wvlet.airframe.http.recorder

import org.yaml.snakeyaml.Yaml
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.http.{Http, HttpHeader, HttpStatus}
import wvlet.airframe.http.HttpMessage.Response
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.recorder.HttpRequestMatcher.PathOnlyMatcher
import wvlet.airframe.json._
import wvlet.airspec.AirSpec

import scala.util.{Random, Using}

/**
  */
class HttpRecorderTest extends AirSpec {
  private def orderInsensitveHash(m: Map[String, Seq[String]]): Int = {
    m.map { x => s"${x._1}:${x._2}".hashCode }
      .reduce { (xor, next) => xor ^ next }
  }

  private def withClient[U](localAddr: String)(body: SyncClient => U): U = {
    val client = Http.client.withRetryContext(_.noRetry).newSyncClient(localAddr)
    try {
      body(client)
    } finally {
      client.close()
    }
  }

  test("https connection") {
    Using.resource(Http.client.newSyncClient("wvlet.org:443")) { client =>
      val req  = Http.GET("/airframe/")
      val resp = client.send(req)
      debug(resp)
    }
  }

  test("start HTTP recorder") {
    val recorderConfig =
      HttpRecorderConfig(recorderName = "wvlet.org", destUri = "https://wvlet.org", sessionName = "airframe")
    val path = "/airframe/"

    val response: Response =
      withResource(HttpRecorder.createRecordingServer(recorderConfig, dropExistingSession = true)) { server =>
        withClient(server.localAddress) { client =>
          client.send(Http.GET(path))
        }
      }
    val replayResponse: Response = withResource(HttpRecorder.createServer(recorderConfig)) { server =>
      withClient(server.localAddress) { client =>
        client.send(Http.GET(path))
      }
    }

    response.status shouldBe replayResponse.status
    replayResponse.getHeader("X-Airframe-Record-Time") shouldBe defined
    orderInsensitveHash(response.header.toMultiMap) shouldBe orderInsensitveHash(
      replayResponse.header.remove("X-Airframe-Record-Time").toMultiMap
    )
    response.contentString shouldBe replayResponse.contentString

    // Check non-recorded response
    val errorResponse = withResource(HttpRecorder.createServer(recorderConfig)) { server =>
      withClient(server.localAddress) { client =>
        client.sendSafe(Http.GET("/non-recorded-path.html"))
      }
    }
    // Not found
    errorResponse.statusCode shouldBe 404
  }

  test("switch recording/replaying") {
    val recorderConfig =
      HttpRecorderConfig(destUri = "https://wvlet.org", sessionName = "airframe-path-through")

    // Recording
    withResource(HttpRecorder.createRecorderProxy(recorderConfig, dropExistingSession = true)) { server =>
      withClient(server.localAddress) { client =>
        val request = Http.GET("/airframe/")
        val r1      = client.sendSafe(request)
        r1.getHeader("X-Airframe-Record-Time") shouldBe empty
        val r2 = client.sendSafe(request)
        r2.getHeader("X-Airframe-Record-Time") shouldBe empty
      }
    }

    // Replaying
    val replayConfig =
      HttpRecorderConfig(destUri = "https://wvlet.org", sessionName = "airframe-path-through")
    withResource(HttpRecorder.createRecorderProxy(replayConfig)) { server =>
      withClient(server.localAddress) { client =>
        val request = Http.GET("/airframe/")
        val r1      = client.sendSafe(request)
        val r2      = client.sendSafe(request)
        r1.getHeader("X-Airframe-Record-Time") shouldBe defined
        r2.getHeader("X-Airframe-Record-Time") shouldBe defined
      }
    }
  }

  test("programmable server") {
    val response = withResource(HttpRecorder.createInMemoryServer(HttpRecorderConfig())) { server =>
      server.clearSession

      val request  = Http.GET("/index.html")
      val response = Http.response().withContent("Hello World!")
      server.recordIfNotExists(request, response)

      withClient(server.localAddress) { client =>
        val request = Http.GET("/index.html")
        client.sendSafe(request)
      }
    }

    response.status shouldBe HttpStatus.Ok_200
    response.contentString shouldBe "Hello World!"
  }

  test("delete expired records") {
    val recorderConfig = HttpRecorder.config
      .withDestUri("https://wvlet.org")
      .withSessionName("airframe")
      .withExpirationTime("1s")

    val path = "/airframe/"
    withResource(new HttpRecordStore(recorderConfig, dropSession = true)) { store =>
      store.numRecordsInSession shouldBe 0
      store.record(Http.GET("/airframe"), Http.response())
      store.numRecordsInSession shouldBe 1
    }

    // Wait until expiration
    Thread.sleep(1000)
    withResource(new HttpRecordStore(recorderConfig)) { store => store.numRecordsInSession shouldBe 0 }
  }

  test("support binary contents") {
    val storeConfig = HttpRecorderConfig(destUri = "localhost", sessionName = "binary-test")
    val store       = new HttpRecordStore(storeConfig, dropSession = true)

    val binaryRequestData = new Array[Byte](512)
    Random.nextBytes(binaryRequestData)
    val binaryResponseData = new Array[Byte](1024)
    Random.nextBytes(binaryResponseData)
    val binaryResponse = Http
      .response()
      .withContentType(HttpHeader.MediaType.OctetStream)
      .withContent(binaryResponseData)
      .withContentLength(binaryResponseData.length)

    val request = Http
      .GET("/test")
      .withContentType(HttpHeader.MediaType.OctetStream)
      .withContent(binaryRequestData)
    store.record(request, binaryResponse)

    store.findNext(request) match {
      case None         => fail()
      case Some(record) =>
        // Check binary request
        HttpRecordStore.decodeFromBase64(record.requestBody) shouldBe binaryRequestData

        // Check binary response
        val r = record.toResponse
        r.contentLength shouldBe Some(1024)
        val arr = r.contentBytes
        arr shouldBe binaryResponseData
    }
  }

  test("support simple request matcher") {
    val config = HttpRecorderConfig(requestMatcher = PathOnlyMatcher)
    withResource(HttpRecorder.createInMemoryServer(config)) { server =>
      val request = Http
        .GET("/airframe")
        .withAccept("application/v1+json")
      val response = Http
        .response()
        .withContent("hello airframe")
      server.record(request, response)

      withClient(server.localAddress) { client =>
        val request = Http.GET("/airframe")

        // It should match by ignoring http headers
        val r = client.send(request)
        r.contentString shouldBe "hello airframe"
      }
    }
  }

  test("dump http record store") {
    val config = HttpRecorderConfig(requestMatcher = PathOnlyMatcher)
    withResource(HttpRecorder.createInMemoryServer(config)) { server =>
      val request1 = Http
        .GET("/airframe")
        .withAccept("application/v1+json")
      val response1 = Http
        .response()
        .withContent("hello airframe")
      server.record(request1, response1)

      val binaryRequestData = new Array[Byte](512)
      Random.nextBytes(binaryRequestData)
      val binaryResponseData = new Array[Byte](1024)
      Random.nextBytes(binaryResponseData)
      val response2 = Response()
        .withContentType(HttpHeader.MediaType.OctetStream)
        .withContent(binaryResponseData)
        .withContentLength(binaryResponseData.length)

      val request2 = Http
        .GET("/test")
        .withContent(binaryRequestData)
        .withContentType(HttpHeader.MediaType.OctetStream)
      server.record(request2, response2)

      val yaml = new Yaml().load[java.util.List[java.util.Map[String, AnyRef]]](server.dumpSessionAsYaml)
      yaml.get(0).get("path") shouldBe "/airframe"
      yaml.get(0).get("responseBody") shouldBe "hello airframe"

      yaml.get(1).get("path") shouldBe "/test"
      yaml.get(1).get("responseBody") shouldBe HttpRecordStore.encodeToBase64(binaryResponseData)

      val jsonLines = server.dumpSessionAsJson.split("\n").map(JSON.parse)
      (jsonLines(0) / "path").toStringValue shouldBe "/airframe"
      (jsonLines(0) / "responseBody").toStringValue shouldBe "hello airframe"

      (jsonLines(1) / "path").toStringValue shouldBe "/test"
      (jsonLines(1) / "responseBody").toStringValue shouldBe HttpRecordStore.encodeToBase64(
        binaryResponseData
      )
    }
  }
}
