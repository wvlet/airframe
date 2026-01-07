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
import wvlet.airframe.http.{Http, HttpLogger, HttpServer, RPC, RxRouter}
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.surface.secret
import wvlet.airspec.AirSpec

/**
  * Test for @secret annotation handling in HTTP access logs, including collections (Seq) and Option types.
  */
object SecretLogFilterTest extends AirSpec {

  // Test data models with @secret annotation
  case class Message(role: String, @secret content: String)
  case class ChatRequest(
      model: String,
      messages: Seq[Message],
      @secret systemPrompt: Option[String] = None
  )
  case class UserData(id: Int, @secret pii: String)

  @RPC
  class SecretTestRPC {
    def secretArg(@secret password: String, userData: UserData): String = "ok"
    def secretInSeq(request: ChatRequest): String                       = "ok"
  }

  private var serverLogger: InMemoryHttpLogger = null

  initDesign {
    _.add(
      Netty.server
        .withRouter(RxRouter.of[SecretTestRPC])
        .withHttpLogger { config =>
          serverLogger = new InMemoryHttpLogger(config)
          serverLogger
        }
        .design
        .bind[SyncClient].toProvider { (server: HttpServer) =>
          Http.client.newSyncClient(server.localAddress)
        }
    )
  }

  test("hide @secret args in direct parameters") { (client: SyncClient) =>
    serverLogger.clear()
    client.send(
      Http
        .POST("/wvlet.airframe.http.netty.SecretLogFilterTest.SecretTestRPC/secretArg")
        .withJson("""{"password":"secret-password","userData":{"id":1,"pii":"confidential-data"}}""")
    )

    val logs = serverLogger.getLogs
    logs.nonEmpty shouldBe true
    val log = logs.head
    debug(log)

    val rpcArgs = log("rpc_args").asInstanceOf[Map[String, Any]]
    // password should be hidden
    rpcArgs.contains("password") shouldBe false
    // userData should be present but pii should be hidden
    val userData = rpcArgs("userData").asInstanceOf[Map[String, Any]]
    userData("id") shouldBe 1
    userData.contains("pii") shouldBe false
  }

  test("hide @secret args in Seq and Option") { (client: SyncClient) =>
    serverLogger.clear()
    client.send(
      Http
        .POST("/wvlet.airframe.http.netty.SecretLogFilterTest.SecretTestRPC/secretInSeq")
        .withJson(
          """{"request":{"model":"gpt-4","messages":[{"role":"user","content":"secret message"},{"role":"assistant","content":"secret response"}],"systemPrompt":"secret system prompt"}}"""
        )
    )

    val logs = serverLogger.getLogs
    logs.nonEmpty shouldBe true
    val log = logs.head
    debug(log)

    val rpcArgs = log("rpc_args").asInstanceOf[Map[String, Any]]
    val request = rpcArgs("request").asInstanceOf[Map[String, Any]]

    // model should be visible
    request("model") shouldBe "gpt-4"

    // systemPrompt (Option with @secret) should be hidden
    request.contains("systemPrompt") shouldBe false

    // messages should be present but content should be hidden in each element
    // After traversal, Seq elements should be converted to Maps with @secret fields removed
    val messages = request("messages").asInstanceOf[Seq[?]]
    messages.size shouldBe 2

    // Each message should be a Map (traversed), not the original Message object
    messages.head match {
      case m: Map[?, ?] =>
        val msgMap = m.asInstanceOf[Map[String, Any]]
        msgMap("role") shouldBe "user"
        msgMap.contains("content") shouldBe false // @secret field should be hidden
      case other =>
        fail(s"Expected Map but got ${other.getClass.getName}. Seq elements should be traversed to hide @secret fields.")
    }

    messages(1) match {
      case m: Map[?, ?] =>
        val msgMap = m.asInstanceOf[Map[String, Any]]
        msgMap("role") shouldBe "assistant"
        msgMap.contains("content") shouldBe false // @secret field should be hidden
      case other =>
        fail(s"Expected Map but got ${other.getClass.getName}. Seq elements should be traversed to hide @secret fields.")
    }
  }
}
