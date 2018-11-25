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
package wvlet.airframe
import wvlet.log.LogSupport

import scala.util.Random

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
  // Should be shared among child sessions
  trait ThreadManager {
    val threadId = Random.nextInt(100000)
  }

  trait HttpServer extends LogSupport {
    private val currentSession = bind[Session]
    val threadManager          = bind[ThreadManager]

    def handle(req: HttpRequest) = {
      debug(s"get request:${req}")
      val childDesign = newDesign
        .bind[HttpRequest].toInstance(req)
        .bind[User].toInstance(User(req.userName))

      val childSession = currentSession.newChildSession(childDesign)
      childSession.start {
        val handler = req.path match {
          case "/info" =>
            childSession.build[InfoHandler]
          case "/query" =>
            childSession.build[QueryHandler]
        }
        val response = handler.handle
        info(response)
        response
      }
    }
  }

  case class HandlerResult(parentThreadId: Int, path: String, user: User, authorized: Boolean)

  trait HttpRequestHandler extends LogSupport with UserAuth {
    val req  = bind[HttpRequest]
    val user = bind[User]
    val threadManager = bind[ThreadManager]
      .onStart { x =>
        throw new IllegalStateException("Child session manager must not the parent thread manager")
      }

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
}

/**
  *
  */
class ChildSessionTest extends AirframeSpec {
  import ChildSessionTest._
  "support creating a child session" in {
    serverDesign.build[HttpServer] { server =>
      val parentThreadId = server.threadManager.threadId
      server.handle(HttpRequest("/info", "leo")) shouldBe HandlerResult(parentThreadId, "/info", User("leo"), false)
      server.handle(HttpRequest("/info", "yui")) shouldBe HandlerResult(parentThreadId, "/info", User("yui"), false)
      server.handle(HttpRequest("/query", "aina")) shouldBe HandlerResult(parentThreadId, "/query", User("aina"), true)
    }
  }
}
