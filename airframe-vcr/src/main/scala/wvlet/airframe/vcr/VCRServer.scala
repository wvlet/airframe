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
package wvlet.airframe.vcr
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import wvlet.airframe.codec.{JSONCodec, MessageCodec}
import wvlet.airframe.http.finagle.FinagleServer.FinagleService
import wvlet.airframe.http.finagle.{FinagleServer, FinagleServerConfig}
import wvlet.airframe.jdbc.{DbConfig, SQLiteConnectionPool}
import wvlet.log.io.IOUtil

case class VCRConfig(folder: String = "fixtures", sessionName: String = "default")

object VCR {

  /**
    * Creates an HTTP server that returns VCR recorded responses.
    * If no matching response is found, it falls back to the original server, and
    * records the result.
    */
  def newPassThroughServer(vcrConfig: VCRConfig, fallBackUri: String): Unit = {
    val port          = IOUtil.unusedPort
    val finagleConfig = FinagleServerConfig(port)
    new FinagleServer(finagleConfig, new VCRServer(vcrConfig))
  }

  /**
    * Creates an HTTP server that returns VCR recorded responses.
    * If no matching record is found, use the fallBack handler.
    */
  def newReplayOnlyServer(vcrConfig: VCRConfig, fallBack: Request => Response) {}

}

case class VCR(session: String, path: String, method: String, code: Int, header: Map[String, String], requestBody: String, content: String, createdAt: Long)

object VCRServer {

  case class RequestKey(method: String, uri: String, contentBody: Option[String])

  private val requestKeyCodec = MessageCodec.of[RequestKey]

  def extractUriAndBody(request: Request): String = {
    val content = request.getContentString()
    val contentBody = if (content.isEmpty) {
      None
    } else {
      Some(content)
    }
    val r = RequestKey(request.method.toString(), request.uri, contentBody)
    JSONCodec.toJson(requestKeyCodec.toMsgPack(r))
  }

}

class VCRServer(vcrConfig: VCRConfig, requestKey: Request => String = VCRServer.extractUriAndBody)
    extends FinagleService {

  override def apply(request: Request): Future[Response] = {
    val key = requestKey(request)

  }
}

class VCRRecorder(vcrConfig: VCRConfig) extends AutoCloseable {
  private val connectionPool = new SQLiteConnectionPool(
    DbConfig.ofSQLite(s"${vcrConfig.folder}/${vcrConfig.sessionName}"))

  def get(key: String): Option[VCR] = {
    connectionPool.queryWith(
      s"""
         |select
       """.stripMargin)

  }
}

  override def close(): Unit = {
    connectionPool.stop
  }
}
