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
import com.twitter.finagle.http.{Request, Response}
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
    val response: Response = withResource(HttpRecorder.createRecordingServer(recorderConfig)) { server =>
      server.start
      withClient(server.localAddress) { client =>
        val response = client(Request(path)).map { response =>
          debug(response)
          response
        }
        Await.result(response)
      }
    }

    val replayResponse: Response = withResource(HttpRecorder.createReplayServer(recorderConfig)) { server =>
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
    orderInsensitveHash(response.headerMap.toMap) shouldBe orderInsensitveHash(replayResponse.headerMap.toMap)
    response.contentString shouldBe replayResponse.contentString
  }
}
