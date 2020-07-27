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
import java.io.{ByteArrayInputStream, InputStream}

import io.grpc.MethodDescriptor.Marshaller
import io.grpc.stub.ServerCalls
import io.grpc.{MethodDescriptor, ServerServiceDefinition}
import wvlet.airframe.Session
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.control.IO
import wvlet.airframe.http.Router
import wvlet.airframe.http.router.Route
import wvlet.airframe.msgpack.spi.MsgPack

/**
  */
object GrpcServiceBuilder {

  def buildMethodDescriptor(r: Route, codecFactory: MessageCodecFactory): MethodDescriptor[MsgPack, Any] = {
    val b = MethodDescriptor.newBuilder[MsgPack, Any]()
    // TODO setIdempotent, setSafe, sampling, etc.
    b.setType(MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(s"${r.serviceName}/${r.methodSurface.name}")
      .setRequestMarshaller(RPCRequestMarshaller)
      .setResponseMarshaller(
        new RPCResponseMarshaller[Any](
          codecFactory.of(r.returnTypeSurface).asInstanceOf[MessageCodec[Any]]
        )
      )
      .build()
  }

  def buildService(
      router: Router,
      session: Session,
      codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
  ): Seq[ServerServiceDefinition] = {
    val services = for ((serviceName, routes) <- router.routes.groupBy(_.serviceName)) yield {
      val routeAndMethods = for (route <- routes) yield {

        (route, buildMethodDescriptor(route, codecFactory))
      }

      val serviceBuilder = ServerServiceDefinition
        .builder(serviceName)

      for ((r, m) <- routeAndMethods) {
        // TODO Support Client/Server Streams
        val controller = session.getInstanceOf(r.controllerSurface)
        serviceBuilder.addMethod(
          m,
          ServerCalls.asyncUnaryCall(new RPCRequestHandler[Any](controller, r.methodSurface, codecFactory))
        )
      }
      val serviceDef = serviceBuilder.build()
      serviceDef
    }

    services.toSeq
  }

  object RPCRequestMarshaller extends Marshaller[MsgPack] {
    override def stream(value: MsgPack): InputStream = {
      new ByteArrayInputStream(value)
    }
    override def parse(stream: InputStream): MsgPack = {
      IO.readFully(stream)
    }
  }

  class RPCResponseMarshaller[A](codec: MessageCodec[A]) extends Marshaller[A] {
    override def stream(value: A): InputStream = {
      new ByteArrayInputStream(codec.toMsgPack(value))
    }
    override def parse(stream: InputStream): A = {
      codec.fromMsgPack(stream.readAllBytes())
    }
  }

}
