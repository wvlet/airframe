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

case class OpenAPI(
    openapi: String = "3.0.3",
    info: Info,
    paths: Map[String, Map[String, PathItem]],
    components: Components
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
      requestBody: Option[RequestBody],
      responses: Seq[Response]
  )

  case class Parameter(
      name: String,
      in: In,
      description: Option[String] = None,
      required: Boolean = false,
      deprecated: Boolean = false,
      allowEmptyValue: Boolean = false
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
      schema: SchemaOrRef,
      encoding: Map[String, Encoding] = Map.empty
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

  case class Response(
      description: String,
      headers: Map[String, Header] = Map.empty,
      // Status code string -> MediaType
      content: Map[String, MediaType] = Map.empty
  )

  case class Header()

  case class Components(
      schemas: Map[String, Schema] = Map.empty
  )
}
