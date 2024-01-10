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

/**
  * A common trait for Http server implementations
  */
trait HttpServer extends AutoCloseable {

  /**
    * Await and block until the server is terminated. If the server is already terminated, it will return immediately.
    */
  def awaitTermination(): Unit

  /**
    * Stop the server
    */
  def stop(): Unit

  /**
    * Stop the server. When using Airframe DI, this method will be called automatically.
    */
  override def close(): Unit = stop()

  /**
    * Return the local server address in (host):(port) format. e.g., localhost:8080. This method can be used for
    * creating a new Http client for the server. For example:
    * {{{
    *   Netty.server.withRouter(router).start { server =>
    *     Using.resource(Http.client.newSyncClient(server.localAddress)) { client =>
    *        val resp = client.send(Http.GET("/hello"))
    *     }
    *   }
    * }}}
    * @return
    */
  def localAddress: String
}
