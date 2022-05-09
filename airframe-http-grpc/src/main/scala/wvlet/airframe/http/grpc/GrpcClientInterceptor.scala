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
package wvlet.airframe.http.grpc

import io.grpc.stub.MetadataUtils
import io.grpc.{Channel, ClientInterceptors, Metadata}
import wvlet.airframe.http.RPCEncoding

object GrpcClientInterceptor {
  def wrap(c: Channel, encoding: RPCEncoding = RPCEncoding.MsgPack): Channel = {
    import GrpcContext._
    val newHeaders = new Metadata()
    newHeaders.put(
      Metadata.Key.of("x-airframe-client-version", Metadata.ASCII_STRING_MARSHALLER),
      wvlet.airframe.http.BuildInfo.version
    )
    newHeaders.setAccept(encoding.applicationType)
    val interceptor = MetadataUtils.newAttachHeadersInterceptor(newHeaders)
    ClientInterceptors.intercept(c, interceptor)
  }
}
