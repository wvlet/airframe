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
package wvlet.airframe.http.netty

import wvlet.airframe.http.HttpLogger.InMemoryHttpLogger
import wvlet.airframe.http.{Http, HttpLogger, HttpServer, RPC, RPCContext, RxRouter}
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.netty.NettyRxFilterTest.router1
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

import scala.collection.immutable.ListMap

object NettyLoggingTest extends AirSpec {

  @RPC
  class MyRPC extends LogSupport {
    def hello(): Unit = {
      RPCContext.current.setThreadLocal("user", "xxxx_yyyy")
      debug("hello rpc")
    }
  }

  private var clientLogger: InMemoryHttpLogger = null
  private var serverLogger: InMemoryHttpLogger = null

  initDesign {
    _.add(
      Netty.server
        .withRouter(RxRouter.of[MyRPC])
        .withHttpLogger { config =>
          serverLogger = new InMemoryHttpLogger(config)
          serverLogger
        }
        .withName("log-test-server")
        .withExtraLogEntries { () =>
          val m = ListMap.newBuilder[String, Any]
          RPCContext.current.getThreadLocal[String]("user").foreach { v =>
            m += "user" -> v
          }
          m += ("custom_log_entry" -> "test")
          m.result
        }
        .design
        .bind[SyncClient].toProvider { (server: HttpServer) =>
          Http.client
            .withHttpLogger { config =>
              clientLogger = new InMemoryHttpLogger(config)
              clientLogger
            }
            .withExtraLogEntries(() => Map("custom_log_entry" -> "log-test-client"))
            .newSyncClient(server.localAddress)
        }
    )
  }

  test("add server custom log") { (syncClient: SyncClient) =>
    syncClient.send(Http.POST("/wvlet.airframe.http.netty.NettyLoggingTest.MyRPC/hello"))
    val logEntry = serverLogger.getLogs.head
    debug(logEntry)
    logEntry shouldContain ("server_name"      -> "log-test-server")
    logEntry shouldContain ("custom_log_entry" -> "test")
    logEntry shouldContain ("user"             -> "xxxx_yyyy")

    test("add client custom log") {
      val clientLogEntry = clientLogger.getLogs.head
      debug(clientLogEntry)
      clientLogEntry shouldContain ("custom_log_entry" -> "log-test-client")
    }

  }

}
