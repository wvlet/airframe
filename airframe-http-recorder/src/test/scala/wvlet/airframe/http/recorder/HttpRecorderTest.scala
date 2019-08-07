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
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util.Await
import wvlet.airframe.AirframeSpec
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.http.finagle.FinagleServer.FinagleService

/**
  *
  */
class HttpRecorderTest extends AirframeSpec {

  def orderInsensitveHash(m: Map[String, String]): Int = {
    m.map { x =>
        s"${x._1}:${x._2}".hashCode
      }
      .reduce { (xor, next) =>
        xor ^ next
      }
  }

  def withClient[U](addr: String)(body: FinagleService => U): U = {
    val client = Http.client.newService(addr)
    try {
      body(client)
    } finally {
      client.close()
    }
  }

  "start HTTP recorder" in {
    val recorderConfig =
      HttpRecorderConfig(destUri = "https://wvlet.org", sessionName = "airframe")
    val path = "/airframe/"
    val response: Response =
      withResource(HttpRecorder.createRecordOnlyServer(recorderConfig, dropExistingSession = true)) { server =>
        server.start
        withClient(server.localAddress) { client =>
          val response = client(Request(path)).map { response =>
            debug(response)
            response
          }
          Await.result(response)
        }
      }

    val replayResponse: Response = withResource(HttpRecorder.createReplayOnlyServer(recorderConfig)) { server =>
      server.start
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
      replayResponse.headerMap.toMap - "X-Airframe-Record-Time")
    response.contentString shouldBe replayResponse.contentString

    // Check non-recorded response
    val errorResponse = withResource(HttpRecorder.createReplayOnlyServer(recorderConfig)) { server =>
      server.start
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

  "switch recoding/replaying" taggedAs ("path-through") in {
    val recorderConfig =
      HttpRecorderConfig(destUri = "https://wvlet.org", sessionName = "airframe-path-through")

    // Recording
    withResource(HttpRecorder.createRecorderProxy(recorderConfig, dropExistingSession = true)) { server =>
      server.start
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
      server.start
      withClient(server.localAddress) { client =>
        val request = Request("/airframe/")
        val r1      = Await.result(client(request))
        val r2      = Await.result(client(request))
        r1.headerMap.get("X-Airframe-Record-Time") shouldBe defined
        r2.headerMap.get("X-Airframe-Record-Time") shouldBe defined
      }
    }

  }

  "programmable server" in {
    val response = withResource(HttpRecorder.createProgrammableServer() { recorder =>
      val request = Request("/index.html")

      val response = Response()
      response.setContentString("Hello World!")

      recorder.record(request, response)

    }) { server =>
      server.start
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

  "delete expired records" in {
    val recorderConfig =
      HttpRecorderConfig(destUri = "https://wvlet.org",
                         sessionName = "airframe",
                         // Expire immediately
                         expirationTime = "1s")

    val path = "/airframe/"
    withResource(new HttpRecordStore(recorderConfig, dropSession = true)) { store =>
      store.numRecordsInSession shouldBe 0
      store.record(Request("/airframe"), Response())
      store.numRecordsInSession shouldBe 1
    }

    // Wait until expiration
    Thread.sleep(1000)
    withResource(new HttpRecordStore(recorderConfig)) { store =>
      store.numRecordsInSession shouldBe 0
    }
  }

}
