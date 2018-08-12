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
package wvlet.airframe.rest

import java.util.UUID

import javax.ws.rs._
import wvlet.log.LogSupport

object ServiceExample {
  case class User(id: String, name: String)
  case class CreateUserRequest(name: String)
}

/**
  *
  */
trait ServiceExample extends LogSupport {
  import ServiceExample._

  @GET
  @Path("/user/:id")
  def getUser(id: String): User = {
    val u = User(id, "leo")
    info(s"get ${u}")
    u
  }

  @POST
  @Path("/user")
  def newUser(userUpdateRequest: CreateUserRequest): User = {
    // Support mapping JSON body message -> MsgPack -> Object
    val newUser = User(UUID.randomUUID().toString, userUpdateRequest.name)
    info(s"create user: ${newUser}")
    newUser
  }

  @DELETE
  @Path("/user/:id")
  def deleteUser(id: String): Unit = {
    info(s"delete ${id}")
  }

  @PUT
  @Path("/user/:id")
  def updateUser(id: String, httpRequest: HttpRequest): Unit = {
    info(s"id: ${id}, ${httpRequest.contentString}")
  }
}
