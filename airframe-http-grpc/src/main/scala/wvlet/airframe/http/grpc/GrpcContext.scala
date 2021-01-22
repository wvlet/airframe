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

import io.grpc._

object GrpcContext {
  private[grpc] val contextKey = Context.key[GrpcContext]("grpc_context")

  /**
    * Get the current GrpcContext. If it returns None, it means this method is called outside gRPC's local thread for processing the request
    *
    * @return
    */
  def current: Option[GrpcContext] = Option(contextKey.get())

  private[grpc] val KEY_ACCEPT = Metadata.Key.of("accept", Metadata.ASCII_STRING_MARSHALLER)

  private[grpc] implicit class RichMetadata(val m: Metadata) extends AnyVal {
    def accept: String = Option(m.get(KEY_ACCEPT)).getOrElse(GrpcEncoding.ApplicationMsgPack)
    def setAccept(s: String): Unit = {
      m.removeAll(KEY_ACCEPT)
      m.put(KEY_ACCEPT, s)
    }
  }

}

import GrpcContext._

case class GrpcContext(
    authority: Option[String],
    attributes: Attributes,
    metadata: Metadata,
    descriptor: MethodDescriptor[_, _]
) {
  // Return the accept header
  def accept: String = metadata.accept
}
