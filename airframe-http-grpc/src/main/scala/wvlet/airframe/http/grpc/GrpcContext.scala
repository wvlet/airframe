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
import wvlet.airframe.http.{Http, HttpMessage, RPCContext, RPCEncoding}
import wvlet.log.LogSupport

import scala.collection.mutable

object GrpcContext {
  private[grpc] val contextKey = Context.key[GrpcContext]("grpc_context")

  /**
    * Get the current GrpcContext. If it returns None, it means this method is called outside gRPC's local thread for
    * processing the request
    *
    * @return
    */
  def current: Option[GrpcContext] = Option(contextKey.get())

  private[grpc] def currentEncoding = current.map(_.encoding).getOrElse(RPCEncoding.MsgPack)

  private[grpc] val KEY_ACCEPT       = Metadata.Key.of("accept", Metadata.ASCII_STRING_MARSHALLER)
  private[grpc] val KEY_CONTENT_TYPE = Metadata.Key.of("content-type", Metadata.ASCII_STRING_MARSHALLER)

  private[grpc] implicit class RichMetadata(val m: Metadata) extends AnyVal {
    def accept: String = Option(m.get(KEY_ACCEPT)).getOrElse(RPCEncoding.ApplicationMsgPack)
    def setAccept(s: String): Unit = {
      m.removeAll(KEY_ACCEPT)
      m.put(KEY_ACCEPT, s)
    }
    def setContentType(s: String): Unit = {
      m.removeAll(KEY_CONTENT_TYPE)
      m.put(KEY_CONTENT_TYPE, s)
    }
  }

}

import GrpcContext._

case class GrpcContext(
    authority: Option[String],
    attributes: Attributes,
    metadata: Metadata,
    descriptor: MethodDescriptor[_, _]
) extends RPCContext
    with LogSupport {

  // Grpc doesn't provide a mutable thread-local stage, so create our own TLS here.
  private lazy val tls =
    ThreadLocal.withInitial[collection.mutable.Map[String, Any]](() => mutable.Map.empty[String, Any])

  private def storage: collection.mutable.Map[String, Any] = {
    tls.get()
  }

  // Return the accept header
  def accept: String = metadata.accept
  def encoding: RPCEncoding = accept match {
    case RPCEncoding.ApplicationJson =>
      // Json input
      RPCEncoding.JSON
    case _ =>
      // Use msgpack by default
      RPCEncoding.MsgPack
  }

  override def setThreadLocal[A](key: String, value: A): Unit = {
    storage.put(key, value)
  }

  override def getThreadLocal[A](key: String): Option[A] = {
    storage.get(key).asInstanceOf[Option[A]]
  }

  override def httpRequest: HttpMessage.Request = {
    import scala.jdk.CollectionConverters._
    var request = Http.POST(s"/${descriptor.getFullMethodName}")
    for (k <- metadata.keys().asScala) {
      request = request.withHeader(k, metadata.get(Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER)))
    }
    request
  }
}
