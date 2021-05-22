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
package wvlet.airframe.legacy
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

import scala.util.Random
import wvlet.airframe._

object ChildSessionTest {
  case class HttpRequest(path: String, userName: String)
  case class User(name: String)
  trait UserAuth {
    // Session-scoped user will be set by a child session
    private val user = bind[User]

    def authorized: Boolean = {
      user.name == "aina"
    }
  }

  val closed = new AtomicBoolean(false)

  // Should be shared among child sessions
  trait ThreadManager {
    val threadId = Random.nextInt(100000)
  }

  trait ThreadManagerService extends LogSupport {
    val threadManager = bind[ThreadManager]
      .onStart { t => debug(s"Started thread manager: ${t.threadId}") }
      .onShutdown { t =>
        if (closed.get()) {
          throw new IllegalStateException("ThreadManager is already closed")
        }
        debug(s"Closing thread manger: ${t.threadId}")
        closed.set(true)
      }
  }

  trait HttpServer extends ThreadManagerService with LogSupport {
    private val currentSession = bind[Session]

    def handle(req: HttpRequest) = {
      if (closed.get()) {
        new IllegalStateException("Thread manager is closed")
      }

      debug(s"get request:${req}")
      val childDesign = newDesign
        .bind[HttpRequest].toInstance(req)
        .bind[User].toInstance(User(req.userName))

      currentSession
        .withChildSession(childDesign) { childSession =>
          val handler = req.path match {
            case "/info" =>
              childSession.build[InfoHandler]
            case "/query" =>
              childSession.build[QueryHandler]
          }
          val response = handler.handle
          debug(response)
          response
        }
    }
  }

  case class HandlerResult(parentThreadId: Int, path: String, user: User, authorized: Boolean)

  val requestCount = new AtomicInteger(0)

  trait HttpRequestHandler extends LogSupport with ThreadManagerService with UserAuth {
    val req = bind[HttpRequest]
      .onShutdown { r => requestCount.incrementAndGet() }
    val user = bind[User]

    def handle: HandlerResult = {
      HandlerResult(threadManager.threadId, req.path, user, authorized)
    }
  }

  trait InfoHandler  extends HttpRequestHandler
  trait QueryHandler extends HttpRequestHandler

  val serverDesign = newDesign
    .bind[ThreadManager].toSingleton
    .bind[HttpServer].toSingleton
    .bind[UserAuth].toSingleton
    .bind[User].toInstance(User("default-user"))
    .noLifeCycleLogging
}

/**
  */
class ChildSessionTest extends AirSpec {
  import ChildSessionTest._
  test("support creating a child session") {
    requestCount.get() shouldBe 0
    serverDesign.build[HttpServer] { server =>
      val parentThreadId = server.threadManager.threadId
      closed.get() shouldBe false
      server.handle(HttpRequest("/info", "leo")) shouldBe HandlerResult(parentThreadId, "/info", User("leo"), false)
      server.handle(HttpRequest("/info", "yui")) shouldBe HandlerResult(parentThreadId, "/info", User("yui"), false)
      server.handle(HttpRequest("/query", "aina")) shouldBe HandlerResult(parentThreadId, "/query", User("aina"), true)
    }
    closed.get() shouldBe true
    requestCount.get() shouldBe 3
  }
}
