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
package wvlet.airframe.http

/**
  */
trait HttpClientBase[F[_], Req, Resp] {
  def get[Resource](resourcePath: String, requestFilter: Req => Req = identity): F[Resource]

  /**
    * Send a get request using the ResourceRequest. ResourceRequest parameters will be expanded as URL query strings
    */
  def getResource[ResourceRequest, Resource](
          resourcePath: String,
          resourceRequest: ResourceRequest,
          requestFilter: Req => Req = identity
  ): F[Resource]
  def getOps[Resource, OperationResponse](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): F[OperationResponse] = getResource[Resource, OperationResponse](resourcePath, resource, requestFilter)
  def list[OperationResponse](
          resourcePath: String,
          requestFilter: Req => Req = identity
  ): F[OperationResponse]
  def post[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): F[Resource]
  def postRaw[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): F[Resp]
  def postOps[Resource, OperationResponse](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): F[OperationResponse]
  def put[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): F[Resource]
  def putRaw[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): F[Resp]
  def putOps[Resource, OperationResponse](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): F[OperationResponse]
  def delete[OperationResponse](
          resourcePath: String,
          requestFilter: Req => Req = identity
  ): F[OperationResponse]
  def deleteRaw(
          resourcePath: String,
          requestFilter: Req => Req = identity
  ): F[Resp]
  def deleteOps[Resource, OperationResponse](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): F[OperationResponse]
  def patch[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): F[Resource]
  def patchRaw[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): F[Resp]
  def patchOps[Resource, OperationResponse](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): F[OperationResponse]

}

trait HttpSyncClientBase[Req, Resp] {
  def get[Resource](resourcePath: String, requestFilter: Req => Req = identity): Resource

  def getResource[ResourceRequest, Resource](
          resourcePath: String,
          resourceRequest: ResourceRequest,
          requestFilter: Req => Req = identity
  ): Resource = {
    getOps[ResourceRequest, Resource](resourcePath, resourceRequest, requestFilter)
  }

  def getOps[Resource, OperationResponse](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): OperationResponse

  def list[OperationResponse](
          resourcePath: String,
          requestFilter: Req => Req = identity
  ): OperationResponse

  def post[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): Resource
  def postRaw[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): Resp
  def postOps[Resource, OperationResponse](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): OperationResponse

  def put[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): Resource
  def putRaw[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): Resp
  def putOps[Resource, OperationResponse](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): OperationResponse

  def delete[OperationResponse](
          resourcePath: String,
          requestFilter: Req => Req = identity
  ): OperationResponse
  def deleteRaw(
          resourcePath: String,
          requestFilter: Req => Req = identity
  ): Resp
  def deleteOps[Resource, OperationResponse](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): OperationResponse

  def patch[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): Resource
  def patchRaw[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): Resp
  def patchOps[Resource, OperationResponse](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): OperationResponse

}

abstract class HttpSyncClientAdapterBase[F[_], Req, Resp](asyncClient: HttpClient[F, Req, Resp])
        extends HttpSyncClient[Req, Resp] {
  protected def awaitF[A](f: F[A]): A = asyncClient.awaitF(f)

  override def get[Resource](resourcePath: String, requestFilter: Req => Req = identity): Resource = {
    awaitF(asyncClient.get[Resource](resourcePath, requestFilter))
  }

  override def getOps[Resource, OperationResponse](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req
  ): OperationResponse = {
    awaitF(asyncClient.getResource[Resource, OperationResponse](resourcePath, resource, requestFilter))
  }

  override def list[OperationResponse](
          resourcePath: String,
          requestFilter: Req => Req = identity
  ): OperationResponse = {
    awaitF(asyncClient.list[OperationResponse](resourcePath, requestFilter))
  }

  override def post[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): Resource = {
    awaitF(asyncClient.post[Resource](resourcePath, resource, requestFilter))
  }

  override def postRaw[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): Resp = {
    awaitF(asyncClient.postRaw[Resource](resourcePath, resource, requestFilter))
  }

  override def postOps[Resource, OperationResponse](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): OperationResponse = {
    awaitF(asyncClient.postOps[Resource, OperationResponse](resourcePath, resource, requestFilter))
  }

  override def put[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): Resource = {
    awaitF(asyncClient.put[Resource](resourcePath, resource, requestFilter))
  }

  override def putRaw[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): Resp = {
    awaitF(asyncClient.putRaw[Resource](resourcePath, resource, requestFilter))
  }

  override def putOps[Resource, OperationResponse](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): OperationResponse = {
    awaitF(asyncClient.putOps[Resource, OperationResponse](resourcePath, resource, requestFilter))
  }

  override def delete[OperationResponse](
          resourcePath: String,
          requestFilter: Req => Req = identity
  ): OperationResponse = {
    awaitF(asyncClient.delete[OperationResponse](resourcePath, requestFilter))
  }

  override def deleteRaw(
          resourcePath: String,
          requestFilter: Req => Req = identity
  ): Resp = {
    awaitF(asyncClient.deleteRaw(resourcePath, requestFilter))
  }

  override def deleteOps[Resource, OperationResponse](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): OperationResponse = {
    awaitF(asyncClient.deleteOps[Resource, OperationResponse](resourcePath, resource, requestFilter))
  }

  override def patch[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): Resource = {
    awaitF(asyncClient.patch[Resource](resourcePath, resource, requestFilter))
  }

  override def patchRaw[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): Resp = {
    awaitF(asyncClient.patchRaw[Resource](resourcePath, resource, requestFilter))
  }

  override def patchOps[Resource, OperationResponse](
          resourcePath: String,
          resource: Resource,
          requestFilter: Req => Req = identity
  ): OperationResponse = {
    awaitF(asyncClient.patchOps[Resource, OperationResponse](resourcePath, resource, requestFilter))
  }

}
