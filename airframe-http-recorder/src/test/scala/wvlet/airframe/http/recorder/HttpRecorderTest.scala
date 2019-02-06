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
import com.twitter.finagle.http.Request
import com.twitter.util.Await
import wvlet.airframe.AirframeSpec
import wvlet.airframe.control.Control.withResource

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

  "start HTTP recorder" in {
    val recorderConfig =
      HttpRecorderConfig(destUri = "https://www.google.com", sessionName = "google")
    val response = withResource(HttpRecorder.createRecordingServer(recorderConfig)) { server =>
      server.start
      val client = Http.client.newService(server.localAddress)
      val response = client(Request("/")).map { response =>
        debug(response)
        response
      }
      Await.result(response)
    }

    val replayResponse = withResource(HttpRecorder.createReplayServer(recorderConfig)) { server =>
      server.start
      val client = Http.client.newService(server.localAddress)
      val response = client(Request("/")).map { response =>
        debug(response)
        response
      }
      Await.result(response)
    }

    response.status shouldBe replayResponse.status
    debug(response.headerMap.map(x => s"${x._1}:${x._2}").mkString("\n"))
    debug(replayResponse.headerMap.map(x => s"${x._1}:${x._2}").mkString("\n"))
    orderInsensitveHash(response.headerMap.toMap) shouldBe orderInsensitveHash(replayResponse.headerMap.toMap)
  }
}
