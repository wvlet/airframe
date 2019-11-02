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
package wvlet.airframe.http.example

import wvlet.airframe.http.{Endpoint, HttpMethod, HttpRequest, HttpRequestAdapter}
import wvlet.log.LogSupport

object ControllerExample {
  case class User(id: String, name: String)
  case class CreateUserRequest(id: String, name: String)
  case class Group(name: String, users: Seq[User])
}

/**
  *
  */
trait ControllerExample extends LogSupport {
  import ControllerExample._

  @Endpoint(path = "/user/:id", method = HttpMethod.GET)
  def getUser(id: String): User = {
    val u = User(id, "leo")
    debug(s"get ${u}")
    u
  }

  // Request body -> function arg (CreateUserRequest) mapping
  @Endpoint(path = "/user", method = HttpMethod.POST)
  def newUser(createUserRequest: CreateUserRequest): User = {
    // Support mapping JSON body message -> MsgPack -> Object
    val newUser = User(createUserRequest.id, createUserRequest.name)
    debug(s"create user: ${newUser}, create request:${createUserRequest}")
    newUser
  }

  @Endpoint(path = "/user/:id", method = HttpMethod.DELETE)
  def deleteUser(id: String): Unit = {
    debug(s"delete ${id}")
  }

  @Endpoint(path = "/user/:id", method = HttpMethod.PUT)
  def updateUser(id: String, httpRequest: HttpRequest[_]): String = {
    debug(s"id: ${id}, ${httpRequest.contentString}")
    httpRequest.contentString
  }

  @Endpoint(path = "/:group/users", method = HttpMethod.GET)
  def groupUsers(group: String): Group = {
    val g = Group(group, Seq(User("10", "leo")))
    debug(s"get ${g}")
    g
  }

  @Endpoint(path = "/:group/user/:id", method = HttpMethod.GET)
  def groupUser(group: String, id: String): Group = {
    val g = Group(group, Seq(User(id, "leo")))
    debug(s"get ${g}")
    g
  }

  @Endpoint(path = "/conflict/users", method = HttpMethod.GET)
  def conflictPath(): Group = {
    val g = Group("xxx", Seq(User("10", "leo")))
    debug(s"get ${g}")
    g
  }

  @Endpoint(path = "/v1/config/entry/*path", method = HttpMethod.GET)
  def getEntry(path: String): String = {
    path
  }

  @Endpoint(path = "/v1/config/info", method = HttpMethod.GET)
  def getInfo(path: String): String = {
    "hello"
  }
}

trait InvalidService {
  @Endpoint(path = "wrong_path")
  def hello: Unit = {}
}

@Endpoint(path = "/v1")
trait PrefixExample {
  @Endpoint(path = "/hello")
  def hello: String = {
    "hello"
  }
}
