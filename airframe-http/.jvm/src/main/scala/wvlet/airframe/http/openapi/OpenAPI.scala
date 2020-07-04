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
package wvlet.airframe.http.openapi

import OpenAPI._
import wvlet.airframe.surface.Union2

case class OpenAPI(
    openapi: String = "3.0.3",
    info: Info,
    paths: Map[String, Map[String, PathItem]],
    components: Option[Components] = None
)

/**
  * A subset of Open API objects necessary for describing Airframe RPC interfaces
  */
object OpenAPI {

  case class Info(
      title: String,
      version: String,
      description: Option[String] = None,
      termsOfService: Option[String] = None
  )

  case class License(name: String, url: Option[String] = None)
  def APL2 = License("Apache 2.0", Some("https://www.apache.org/licenses/LICENSE-2.0.html"))

  case class PathItem(
      summary: String,
      description: String,
      operationId: String,
      parameters: Seq[Parameter] = Seq.empty,
      requestBody: Option[RequestBody] = None,
      // Status Code -> ResponseRef or Response
      responses: Map[String, Union2[Response, ResponseRef]]
  )

  case class Parameter(
      name: String,
      in: In,
      description: Option[String] = None,
      required: Boolean = false,
      deprecated: Option[Boolean] = None,
      allowEmptyValue: Option[Boolean] = None
  )

  sealed trait In

  object In {
    case object query  extends In
    case object header extends In
    case object path   extends In
    case object cookie extends In

    private def all = Seq(query, header, path, cookie)

    def unapply(s: String): Option[In] = {
      all.find(x => x == s.toString)
    }
  }

  case class RequestBody(
      description: Option[String] = None,
      // content-type -> MediaType
      content: Map[String, MediaType],
      required: Boolean = false
  )

  case class MediaType(
      // Scheme or SchemaRef,
      schema: Any,
      encoding: Option[Map[String, Encoding]] = None
  )

  sealed trait SchemaOrRef

  case class SchemaRef(
      `$ref`: String
  ) extends SchemaOrRef
  case class Schema(
      `type`: String,
      required: Option[Seq[String]] = None,
      // property name -> property object
      properties: Map[String, Property] = Map.empty
  ) extends SchemaOrRef

  case class Property(
      `type`: String,
      format: Option[String] = None,
      description: Option[String] = None,
      nullable: Option[Boolean] = None,
      enum: Option[Seq[String]] = None,
      items: Option[Seq[Property]] = None
  )

  case class Encoding()

  sealed trait ResponseOrRef

  case class ResponseRef(
      `$ref`: String
  ) extends ResponseOrRef

  case class Response(
      description: String,
      headers: Map[String, Header] = Map.empty,
      // Status code string -> MediaType
      content: Map[String, MediaType] = Map.empty
  ) extends ResponseOrRef

  case class Header()

  case class Components(
      schemas: Map[String, Schema] = Map.empty,
      responses: Map[String, Response] = Map.empty,
      parameters: Map[String, Parameter] = Map.empty
  )
}
