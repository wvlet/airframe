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

  trait HttpServer extends LogSupport {
    private val session = bind[Session]

    def handle(req: HttpRequest) = {
      warn(s"get request:${req}")
      val childDesign = newDesign
        .bind[HttpRequest].toInstance(req)
        .bind[User].toInstance(User(req.userName))

      val childSession = session.newChildSession(childDesign)
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

  case class HandlerResult(path: String, user: User, authorized: Boolean)

  trait HttpRequestHandler extends LogSupport with UserAuth {
    val req  = bind[HttpRequest]
    val user = bind[User]

    def handle: HandlerResult = {
      HandlerResult(req.path, user, authorized)
    }
  }

  trait InfoHandler  extends HttpRequestHandler
  trait QueryHandler extends HttpRequestHandler

  val serverDesign = newDesign
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
      server.handle(HttpRequest("/info", "leo"))
      server.handle(HttpRequest("/info", "yui"))
      server.handle(HttpRequest("/query", "aina"))
    }
  }
}
