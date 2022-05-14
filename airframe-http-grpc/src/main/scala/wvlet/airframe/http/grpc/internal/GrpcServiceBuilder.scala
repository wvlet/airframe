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
package wvlet.airframe.http.grpc.internal

import io.grpc.stub.ServerCalls
import io.grpc.{MethodDescriptor, ServerServiceDefinition}
import wvlet.airframe.Session
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.http.Router
import wvlet.airframe.http.grpc.{
  GrpcMethod,
  GrpcRequestMarshaller,
  GrpcResponse,
  GrpcResponseMarshaller,
  GrpcServerConfig,
  GrpcService
}
import wvlet.airframe.http.router.Route
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.surface.{MethodParameter, MethodSurface, Surface}
import wvlet.airframe.rx._

import java.util.concurrent.ExecutorService

/**
  */
object GrpcServiceBuilder {
  private implicit class RichMethod(val m: MethodSurface) extends AnyVal {

    private def findClientStreamingArg: Option[MethodParameter] = {
      m.args.find(x => classOf[Rx[_]].isAssignableFrom(x.surface.rawType))
    }

    def grpcMethodType: MethodDescriptor.MethodType = {
      val serverStreaming =
        classOf[Rx[_]].isAssignableFrom(m.returnType.rawType)
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

  def buildGrpcMethod[Req, Resp](
      r: Route,
      requestSurface: Surface,
      codecFactory: MessageCodecFactory
  ): GrpcMethod[Req, Resp] = {
    val desc = buildMethodDescriptor(r, codecFactory)
    GrpcMethod[Req, Resp](
      desc.asInstanceOf[MethodDescriptor[MsgPack, Resp]],
      codecFactory.ofSurface(requestSurface).asInstanceOf[MessageCodec[Req]]
    )
  }

  def buildMethodDescriptor(r: Route, codecFactory: MessageCodecFactory): MethodDescriptor[MsgPack, Any] = {
    val b = MethodDescriptor.newBuilder[MsgPack, Any]()
    // TODO setIdempotent, setSafe, sampling, etc.
    b.setType(r.methodSurface.grpcMethodType)
      .setFullMethodName(s"${r.serviceName}/${r.methodSurface.name}")
      .setRequestMarshaller(GrpcRequestMarshaller)
      .setResponseMarshaller(
        new GrpcResponseMarshaller[Any](
          r.returnTypeSurface match {
            case rx if classOf[Rx[_]].isAssignableFrom(rx.rawType) =>
              codecFactory
                .of(r.returnTypeSurface.typeArgs(0))
                .asInstanceOf[MessageCodec[Any]]
            case _ =>
              codecFactory
                .of(r.returnTypeSurface)
                .asInstanceOf[MessageCodec[Any]]
          }
        )
      )
      .build()
  }

  def buildService(
      config: GrpcServerConfig,
      session: Session
  ): GrpcService = {
    val threadManager: ExecutorService = config.executorProvider(config)
    val requestLogger                  = config.requestLoggerProvider(config)
    val services =
      for ((serviceName, routes) <- config.router.routes.groupBy(_.serviceName))
        yield {
          val routeAndMethods = for (route <- routes) yield {
            (route, buildMethodDescriptor(route, config.codecFactory))
          }

          val serviceBuilder = ServerServiceDefinition.builder(serviceName)

          for ((r, m) <- routeAndMethods) {
            val controller      = session.getInstanceOf(r.controllerSurface)
            val rpcInterfaceCls = Router.findRPCInterfaceCls(r.controllerSurface)
            val requestHandler =
              new GrpcRequestHandler(
                rpcInterfaceCls,
                controller,
                r.methodSurface,
                config.codecFactory,
                threadManager,
                requestLogger
              )
            val serverCall = r.methodSurface.grpcMethodType match {
              case MethodDescriptor.MethodType.UNARY =>
                ServerCalls.asyncUnaryCall(new RPCUnaryMethodHandler(requestHandler))
              case MethodDescriptor.MethodType.SERVER_STREAMING =>
                ServerCalls.asyncServerStreamingCall(new RPCServerStreamingMethodHandler(requestHandler))
              case MethodDescriptor.MethodType.CLIENT_STREAMING =>
                ServerCalls.asyncClientStreamingCall(
                  new RPCClientStreamingMethodHandler(requestHandler, r.methodSurface.clientStreamingRequestType)
                )
              case MethodDescriptor.MethodType.BIDI_STREAMING =>
                ServerCalls.asyncBidiStreamingCall(
                  new RPCBidiStreamingMethodHandler(requestHandler, r.methodSurface.clientStreamingRequestType)
                )
              case other =>
                throw new UnsupportedOperationException(s"${other.toString} is not supported")
            }
            serviceBuilder.addMethod(m, serverCall)
          }
          val serviceDef = serviceBuilder.build()
          serviceDef
        }

    GrpcService(config, threadManager, requestLogger, services.toSeq)
  }

}
