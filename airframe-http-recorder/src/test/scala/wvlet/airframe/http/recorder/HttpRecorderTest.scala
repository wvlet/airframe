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

import com.twitter.finagle.Http
import com.twitter.finagle.http.{MediaType, Request, Response, Status}
import com.twitter.io.Buf
import com.twitter.util.Await
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.http.finagle.FinagleServer.FinagleService
import wvlet.airframe.http.recorder.HttpRequestMatcher.PathOnlyMatcher
import wvlet.airspec.AirSpec

import scala.util.Random

/**
  */
class HttpRecorderTest extends AirSpec {
  private def orderInsensitveHash(m: Map[String, String]): Int = {
    m.map { x => s"${x._1}:${x._2}".hashCode }
      .reduce { (xor, next) => xor ^ next }
  }

  private def withClient[U](addr: String)(body: FinagleService => U): U = {
    val client = Http.client.newService(addr)
    try {
      body(client)
    } finally {
      client.close()
    }
  }

  def `start HTTP recorder`: Unit = {
    val recorderConfig =
      HttpRecorderConfig(recorderName = "wvlet.org", destUri = "https://wvlet.org", sessionName = "airframe")
    val path = "/airframe/"
    val response: Response =
      withResource(HttpRecorder.createRecordOnlyServer(recorderConfig, dropExistingSession = true)) { server =>
        withClient(server.localAddress) { client =>
          val response = client(Request(path)).map { response =>
            debug(response)
            response
          }
          Await.result(response)
        }
      }

    val replayResponse: Response = withResource(HttpRecorder.createReplayOnlyServer(recorderConfig)) { server =>
      withClient(server.localAddress) { client =>
        val response = client(Request(path)).map { response =>
          debug(response)
          response
        }
        Await.result(response)
      }
    }

    response.status shouldBe replayResponse.status
    replayResponse.headerMap.get("X-Airframe-Record-Time") shouldBe defined
    orderInsensitveHash(response.headerMap.toMap) shouldBe orderInsensitveHash(
      replayResponse.headerMap.toMap - "X-Airframe-Record-Time"
    )
    response.contentString shouldBe replayResponse.contentString

    // Check non-recorded response
    val errorResponse = withResource(HttpRecorder.createReplayOnlyServer(recorderConfig)) { server =>
      withClient(server.localAddress) { client =>
        val response = client(Request("/non-recorded-path.html")).map { response =>
          debug(response)
          response
        }
        Await.result(response)
      }
    }
    // Not found
    errorResponse.statusCode shouldBe 404
  }

  def `switch recording/replaying`: Unit = {
    val recorderConfig =
      HttpRecorderConfig(destUri = "https://wvlet.org", sessionName = "airframe-path-through")

    // Recording
    withResource(HttpRecorder.createRecorderProxy(recorderConfig, dropExistingSession = true)) { server =>
      withClient(server.localAddress) { client =>
        val request = Request("/airframe/")
        val r1      = Await.result(client(request))
        r1.headerMap.get("X-Airframe-Record-Time") shouldBe empty
        val r2 = Await.result(client(request))
        r2.headerMap.get("X-Airframe-Record-Time") shouldBe empty
      }
    }

    // Replaying
    val replayConfig =
      HttpRecorderConfig(destUri = "https://wvlet.org", sessionName = "airframe-path-through")
    withResource(HttpRecorder.createRecorderProxy(replayConfig)) { server =>
      withClient(server.localAddress) { client =>
        val request = Request("/airframe/")
        val r1      = Await.result(client(request))
        val r2      = Await.result(client(request))
        r1.headerMap.get("X-Airframe-Record-Time") shouldBe defined
        r2.headerMap.get("X-Airframe-Record-Time") shouldBe defined
      }
    }
  }

  def `programmable server`: Unit = {
    val response = withResource(HttpRecorder.createInMemoryProgrammableServer) { server =>
      server.clearSession

      val request  = Request("/index.html")
      val response = Response()
      response.setContentString("Hello World!")
      server.recordIfNotExists(request, response)

      withClient(server.localAddress) { client =>
        val request = Request("/index.html")

        val response = client(request).map { response =>
          debug(response)
          response
        }
        Await.result(response)
      }
    }

    response.status shouldBe Status.Ok
    response.contentString shouldBe "Hello World!"
  }

  def `delete expired records`: Unit = {
    val recorderConfig = HttpRecorder.config
      .withDestUri("https://wvlet.org")
      .withSessionName("airframe")
      .withExpirationTime("1s")

    val path = "/airframe/"
    withResource(new HttpRecordStore(recorderConfig, dropSession = true)) { store =>
      store.numRecordsInSession shouldBe 0
      store.record(Request("/airframe"), Response())
      store.numRecordsInSession shouldBe 1
    }

    // Wait until expiration
    Thread.sleep(1000)
    withResource(new HttpRecordStore(recorderConfig)) { store => store.numRecordsInSession shouldBe 0 }
  }

  def `support binary contents`: Unit = {
    val storeConfig = HttpRecorderConfig(destUri = "localhost", sessionName = "binary-test")
    val store       = new HttpRecordStore(storeConfig, dropSession = true)

    val binaryRequestData = new Array[Byte](512)
    Random.nextBytes(binaryRequestData)
    val binaryResponseData = new Array[Byte](1024)
    Random.nextBytes(binaryResponseData)
    val binaryResponse = Response()
    binaryResponse.contentType = MediaType.OctetStream
    binaryResponse.content = Buf.ByteArray.Owned(binaryResponseData)
    binaryResponse.contentLength = binaryResponseData.length

    val request = Request("/test")
    request.content = Buf.ByteArray.Owned(binaryRequestData)
    request.contentType = MediaType.OctetStream
    store.record(request, binaryResponse)

    store.findNext(request) match {
      case None         => fail()
      case Some(record) =>
        // Check binary request
        HttpRecordStore.decodeFromBase64(record.requestBody) shouldBe binaryRequestData

        // Check binary response
        val r = record.toResponse
        r.content.length shouldBe 1024
        val arr = new Array[Byte](1024)
        r.content.write(arr, 0)
        arr shouldBe binaryResponseData
    }
  }

  def `support simple request matcher`: Unit = {
    val config = HttpRecorderConfig(requestMatcher = PathOnlyMatcher)
    withResource(HttpRecorder.createInMemoryServer(config)) { server =>
      val request = Request("/airframe")
      request.accept = "application/v1+json"
      val response = Response()
      response.contentString = "hello airframe"
      server.record(request, response)

      withClient(server.localAddress) { client =>
        val request = Request("/airframe")

        // It should match by ignoring http headers
        val r = Await.result(client(request))
        r.contentString shouldBe "hello airframe"
      }
    }
  }
}
