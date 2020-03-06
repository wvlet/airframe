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
package example
import java.util.UUID

import wvlet.airframe.http.{Endpoint, HttpMethod}

case class GetResourceRequest(id: String)
case class ResourceResponse(id: String, data: String)
case class CreateResourceRequest(id: String, data: String)
case class DeleteResourceRequest(id: String)

/**
  *
  */
trait ResourceApi {
  @Endpoint(method = HttpMethod.GET, path = "/v1/resources/:id")
  def getResource(getRequest: GetResourceRequest): ResourceResponse = {
    ResourceResponse(getRequest.id, "hello")
  }

  @Endpoint(method = HttpMethod.POST, path = "/v1/resources")
  def addResource(createResourceRequest: CreateResourceRequest): ResourceResponse = {
    ResourceResponse(createResourceRequest.id, createResourceRequest.data)
  }

  @Endpoint(method = HttpMethod.DELETE, path = "/v1/resources/:id")
  def deleteResource(deleteResourceRequest: DeleteResourceRequest): Unit = {}

  @Endpoint(method = HttpMethod.GET, path = "/v1/resources")
  def listResources: Seq[ResourceResponse] = {
    Seq.empty
  }
}

case class Query(id: String, sql: String)
case class CreateQueryRequest(request_id: String = UUID.randomUUID().toString, sql: String)
case class QueryResultResponse(id: String, nextToken: String)

trait QueryApi {
  @Endpoint(method = HttpMethod.GET, path = "/v1/query")
  def listQueries: Seq[Query]

  @Endpoint(method = HttpMethod.POST, path = "/v1/query")
  def newQuery(createQueryRequest: CreateQueryRequest): QueryResultResponse
}
