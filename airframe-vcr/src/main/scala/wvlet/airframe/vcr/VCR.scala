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
import wvlet.airframe.http.finagle.{FinagleServer, FinagleServerConfig}
import wvlet.log.io.IOUtil

/**
  *
  */
case class VCRConfig(folder: String = "fixtures", sessionName: String = "default")

/**
  * Creates a proxy server for recording and replaying HTTP responses.
  * This is useful for simulate the behavior of Web services, that
  * are too heavy to use in an restricted environment (e.g., CI servers)
  */
object VCR {

  /**
    * Creates an HTTP server that returns VCR recorded responses.
    * If no matching response is found, it falls back to the original server, and
    * records the result.
    */
  def createPassThroughServer(vcrConfig: VCRConfig, fallBackUri: String): Unit = {
    val port          = IOUtil.unusedPort
    val finagleConfig = FinagleServerConfig(port)
    val recorder      = new VCRRecorder(vcrConfig)
    new FinagleServer(finagleConfig, new VCRServer(recorder))
  }

  /**
    * Creates an HTTP server that returns VCR recorded responses.
    * If no matching record is found, use the given fallBack handler.
    */
  def createReplayOnlyServer(vcrConfig: VCRConfig, fallBackHandler: Request => Response): Unit = {
    val port          = IOUtil.unusedPort
    val finagleConfig = FinagleServerConfig(port)
    val recorder      = new VCRRecorder(vcrConfig)
    new FinagleServer(finagleConfig, new VCRServer(recorder))
  }

}
