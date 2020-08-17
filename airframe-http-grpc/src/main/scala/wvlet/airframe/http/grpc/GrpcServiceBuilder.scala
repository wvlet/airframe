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
import wvlet.airframe.rx.Rx
import wvlet.airframe.surface.{MethodParameter, MethodSurface, Surface}
import wvlet.log.LogSupport

/**
  */
object GrpcServiceBuilder {

  private implicit class RichMethod(val m: MethodSurface) extends AnyVal {

    private def findClientStreamingArg: Option[MethodParameter] = {
      m.args.find(x => classOf[Rx[_]].isAssignableFrom(x.surface.rawType))
    }

    def grpcMethodType: MethodDescriptor.MethodType = {
      val serverStreaming = classOf[Rx[_]].isAssignableFrom(m.returnType.rawType)
      val clientStreaming = findClientStreamingArg.isDefined
      (clientStreaming, serverStreaming) match {
        case (false, false) =>
          MethodDescriptor.MethodType.UNARY
        case (true, false) =>
          MethodDescriptor.MethodType.CLIENT_STREAMING
        case (false, true) =>
          MethodDescriptor.MethodType.SERVER_STREAMING
        case (true, true) =>
          MethodDescriptor.MethodType.BIDI_STREAMING
      }
    }
    def clientStreamingRequestType: Surface = {
      findClientStreamingArg.map(_.surface.typeArgs(0)).getOrElse {
        throw new IllegalStateException("unexpected")
      }
    }
  }

  def buildMethodDescriptor(r: Route, codecFactory: MessageCodecFactory): MethodDescriptor[MsgPack, Any] = {
    val b = MethodDescriptor.newBuilder[MsgPack, Any]()
    // TODO setIdempotent, setSafe, sampling, etc.
    b.setType(r.methodSurface.grpcMethodType)
      .setFullMethodName(s"${r.serviceName}/${r.methodSurface.name}")
      .setRequestMarshaller(RPCRequestMarshaller)
      .setResponseMarshaller(
        new RPCResponseMarshaller[Any](
          r.returnTypeSurface match {
            case rx if classOf[Rx[_]].isAssignableFrom(rx.rawType) =>
              codecFactory.of(r.returnTypeSurface.typeArgs(0)).asInstanceOf[MessageCodec[Any]]
            case _ =>
              codecFactory.of(r.returnTypeSurface).asInstanceOf[MessageCodec[Any]]
          }
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

      val serviceBuilder = ServerServiceDefinition.builder(serviceName)

      for ((r, m) <- routeAndMethods) {
        // TODO Support Client/Server Streams
        val controller     = session.getInstanceOf(r.controllerSurface)
        val requestHandler = new RPCRequestHandler(controller, r.methodSurface, codecFactory)
        val serverCall = r.methodSurface.grpcMethodType match {
          case MethodDescriptor.MethodType.UNARY =>
            ServerCalls.asyncUnaryCall(new RPCUnaryMethodHandler(requestHandler))
          case MethodDescriptor.MethodType.SERVER_STREAMING =>
            ServerCalls.asyncServerStreamingCall(new RPCServerStreamingMethodHandler(requestHandler))
          case MethodDescriptor.MethodType.CLIENT_STREAMING =>
            ServerCalls.asyncClientStreamingCall(
              new RPCClientStreamingMethodHandler(requestHandler, r.methodSurface.clientStreamingRequestType)
            )
          case other =>
            throw new UnsupportedOperationException(s"${other.toString} is not yet supported")
        }
        serviceBuilder.addMethod(m, serverCall)
      }
      val serviceDef = serviceBuilder.build()
      serviceDef
    }

    services.toSeq
  }

  object RPCRequestMarshaller extends Marshaller[MsgPack] with LogSupport {
    override def stream(value: MsgPack): InputStream = {
      new ByteArrayInputStream(value)
    }
    override def parse(stream: InputStream): MsgPack = {
      val bytes = IO.readFully(stream)
      bytes
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
