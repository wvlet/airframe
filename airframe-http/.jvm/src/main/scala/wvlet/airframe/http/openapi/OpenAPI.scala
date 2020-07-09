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

import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.Router
import wvlet.airframe.http.openapi.OpenAPI._
import wvlet.airframe.json.YAMLFormatter
import wvlet.airframe.surface.Union2

case class OpenAPI(
    openapi: String = "3.0.3",
    info: Info = Info(title = "API", version = "0.1"),
    paths: Map[String, Map[String, PathItem]],
    components: Option[Components] = None
) {

  /**
    * Set the info of the router
    * @param info
    * @return
    */
  def withInfo(info: Info): OpenAPI = {
    this.copy(info = info)
  }

  def toJSON: String = {
    val codec = MessageCodecFactory.defaultFactoryForJSON.of[OpenAPI]
    codec.toJson(this)
  }

  def toYAML: String = {
    YAMLFormatter.toYaml(toJSON)
  }
}

/**
  * A subset of Open API objects necessary for describing Airframe RPC interfaces
  */
object OpenAPI {

  /**
    * Generate Open API model class from Airframe HTTP/RPC Router definition
    * @param router
    * @return OpenAPI model class
    */
  def ofRouter(router: Router): OpenAPI = {
    OpenAPIGenerator.buildFromRouter(router)
  }

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
      parameters: Option[Seq[ParameterOrRef]] = None,
      requestBody: Option[RequestBody] = None,
      // Status Code -> ResponseRef or Response
      responses: Map[String, Union2[Response, ResponseRef]],
      tags: Option[Seq[String]] = None
  )

  type ParameterOrRef = Union2[Parameter, ParameterRef]

  case class Parameter(
      name: String,
      in: In,
      description: Option[String] = None,
      required: Boolean = false,
      schema: Option[SchemaOrRef] = None,
      deprecated: Option[Boolean] = None,
      allowEmptyValue: Option[Boolean] = None
  ) extends ParameterOrRef {
    override def getElementClass = classOf[Parameter]
  }

  case class ParameterRef(
      `$ref`: String
  ) extends ParameterOrRef {
    override def getElementClass = classOf[ParameterRef]
  }

  sealed trait In

  object In {
    case object query  extends In
    case object header extends In
    case object path   extends In
    case object cookie extends In

    private def all = Seq(query, header, path, cookie)

    def unapply(s: String): Option[In] = {
      all.find(x => x.toString == s)
    }
  }

  case class RequestBody(
      description: Option[String] = None,
      // content-type -> MediaType
      content: Map[String, MediaType],
      required: Boolean = false
  )

  type SchemaOrRef = Union2[Schema, SchemaRef]

  case class MediaType(
      // Scheme or SchemaRef,
      schema: SchemaOrRef,
      encoding: Option[Map[String, Encoding]] = None
  )

  case class SchemaRef(
      `$ref`: String
  ) extends SchemaOrRef {
    override def getElementClass = classOf[SchemaRef]
  }

  case class Schema(
      `type`: String,
      format: Option[String] = None,
      description: Option[String] = None,
      required: Option[Seq[String]] = None,
      // property name -> property object
      properties: Option[Map[String, SchemaOrRef]] = None,
      // For Map-type values
      additionalProperties: Option[SchemaOrRef] = None,
      items: Option[SchemaOrRef] = None,
      nullable: Option[Boolean] = None,
      enum: Option[Seq[String]] = None
  ) extends SchemaOrRef {
    override def getElementClass = classOf[Schema]
  }

  case class Encoding()

  case class ResponseRef(
      `$ref`: String
  ) extends Union2[Response, ResponseRef] {
    def getElementClass = classOf[ResponseRef]
  }

  case class Response(
      description: String,
      headers: Option[Map[String, Header]] = None,
      // Status code string -> MediaType
      content: Map[String, MediaType] = Map.empty
  ) extends Union2[Response, ResponseRef] {
    override def getElementClass = classOf[Response]
  }

  case class Header()

  case class Components(
      schemas: Option[Map[String, SchemaOrRef]] = None,
      responses: Option[Map[String, Response]] = None,
      parameters: Option[Map[String, ParameterOrRef]] = None
  )

}
