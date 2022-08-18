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
package wvlet.airframe.http.grpc.example

import io.grpc.stub.{AbstractBlockingStub, ClientCallStreamObserver, ClientCalls}
import io.grpc.{
  CallOptions,
  Channel,
  Contexts,
  ForwardingServerCallListener,
  Metadata,
  ServerCall,
  ServerCallHandler,
  ServerInterceptor
}
import wvlet.airframe.Design
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.HttpMessage.Request
import wvlet.airframe.http.grpc.internal.{GrpcServiceBuilder, WrappedServerCallListener}
import wvlet.airframe.http.grpc._
import wvlet.airframe.http.router.Route
import wvlet.airframe.http.{Http, HttpStatus, RPC, RPCContext, RPCEncoding, RPCStatus, Router}
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.rx.{Rx, RxStream}
import wvlet.log.LogSupport

@RPC
trait DemoApi extends LogSupport {
  def getContext: String = {
    val ctx = GrpcContext.current
    debug(ctx)
    "Ok"
  }

  def getRPCContext: Option[String] = {
    val ctx = RPCContext.current
    ctx.getThreadLocal[String]("client_id")
  }

  def getRequest: Request = {
    val ctx = RPCContext.current
    ctx.httpRequest
  }

  def hello(name: String): String = {
    s"Hello ${name}!"
  }

  def hello2(name: String, id: Int): String = {
    s"Hello ${name}! (id:${id})"
  }

  def helloStreaming(name: String): RxStream[String] = {
    Rx.sequence("Hello", "Bye").map(x => s"${x} ${name}!")
  }

  def helloClientStreaming(input: RxStream[String]): String = {
    input.toSeq.mkString(", ")
  }

  def helloBidiStreaming(input: RxStream[String]): RxStream[String] = {
    input.map(x => s"Hello ${x}!")
  }

  def helloOpt(opt: Option[String]): String = {
    s"Hello ${opt.getOrElse("unknown")}!"
  }

  def returnUnit(name: String): Unit = {
    // do nothing
    debug(s"hello ${name}")
  }

  def error409Test: String = {
    throw Http.serverException(HttpStatus.Conflict_409).withContent("test message")
  }

  private def throwEx = throw new IllegalArgumentException("syntax error")

  def rpcExceptionTest(suppress: Boolean): String = {
    try {
      throwEx
      ""
    } catch {
      case e: Throwable =>
        val ex = RPCStatus.SYNTAX_ERROR_U3.newException(
          message = "test RPC exception",
          cause = e,
          appErrorCode = 11,
          metadata = Map("retry" -> 0)
        )
        if (suppress) {
          ex.noStackTrace
        }
        throw ex
    }
  }
}

object DemoApi extends LogSupport {

  def demoClientId = "xxx-yyy"

  private def contextTestInterceptor = new ServerInterceptor {
    override def interceptCall[ReqT, RespT](
        call: ServerCall[ReqT, RespT],
        headers: Metadata,
        next: ServerCallHandler[ReqT, RespT]
    ): ServerCall.Listener[ReqT] = {

      new WrappedServerCallListener[Unit, ReqT](
        onInit = {
          val ctx = RPCContext.current
          ctx.setThreadLocal("client_id", demoClientId)
        },
        onDetach = { _ =>
          // do nothing
        },
        next.startCall(call, headers)
      )
    }
  }

  def design: Design = gRPC.server
    .withRouter(router)
    .withName("DemoApi")
    .withInterceptor(contextTestInterceptor)
    .designWithChannel
    .bind[DemoApiClient].toProvider { (channel: Channel) => new DemoApiClient(channel) }

  val router = Router.add[DemoApi]

  private def getRoute(name: String): Route = {
    router.routes.find(_.methodSurface.name == name).getOrElse {
      throw new IllegalArgumentException(s"Route is not found :${name}")
    }
  }

  /**
    * Manually build a gRPC client here as we can't use sbt-airframe.
    * @param channel
    * @param callOptions
    * @param codecFactory
    * @param encoding
    */
  case class DemoApiClient(
      channel: Channel,
      callOptions: CallOptions = CallOptions.DEFAULT,
      codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON,
      encoding: RPCEncoding = RPCEncoding.MsgPack
  ) extends AbstractBlockingStub[DemoApiClient](channel, callOptions) {
    override def build(channel: Channel, callOptions: CallOptions): DemoApiClient = {
      new DemoApiClient(channel, callOptions)
    }
    private val codec = codecFactory.of[Map[String, Any]]
    private val getContextMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("getContext"), codecFactory)
    private val getRPCContextMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("getRPCContext"), codecFactory)
    private val getRequestMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("getRequest"), codecFactory)
    private val helloMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("hello"), codecFactory)
    private val hello2MethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("hello2"), codecFactory)
    private val helloStreamingMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("helloStreaming"), codecFactory)
    private val helloClientStreamingMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("helloClientStreaming"), codecFactory)
    private val helloBidiStreamingMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("helloBidiStreaming"), codecFactory)
    private val helloOptMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("helloOpt"), codecFactory)
    private val returnUnitMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("returnUnit"), codecFactory)
    private val errorTestMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("error409Test"), codecFactory)
    private val rpcExceptionTestMethodDescriptor =
      GrpcServiceBuilder.buildMethodDescriptor(getRoute("rpcExceptionTest"), codecFactory)

    def withEncoding(encoding: RPCEncoding): DemoApiClient = {
      this.copy(encoding = encoding)
    }

    private def encode(map: Map[String, Any]): Array[Byte] = {
      encoding.encodeWithCodec(map, codec)
    }

    private lazy val _channel = GrpcClientInterceptor.wrap(getChannel, encoding)

    def getContext: String = {
      val m = Map.empty[String, Any]
      val resp = ClientCalls
        .blockingUnaryCall(_channel, getContextMethodDescriptor, getCallOptions, encode(m))
      resp.asInstanceOf[String]
    }
    def getRPCContext: Option[String] = {
      val m = Map.empty[String, Any]
      val resp = ClientCalls
        .blockingUnaryCall(_channel, getRPCContextMethodDescriptor, getCallOptions, encode(m))
      resp.asInstanceOf[Option[String]]
    }
    def getRequest: Request = {
      val m = Map.empty[String, Any]
      val resp = ClientCalls
        .blockingUnaryCall(_channel, getRequestMethodDescriptor, getCallOptions, encode(m))
      resp.asInstanceOf[Request]
    }

    def hello(name: String): String = {
      val m = Map("name" -> name)
      val resp = ClientCalls
        .blockingUnaryCall(_channel, helloMethodDescriptor, getCallOptions, encode(m))
      resp.asInstanceOf[String]
    }
    def hello2(name: String, id: Int): String = {
      val m = Map("name" -> name, "id" -> id)
      val resp = ClientCalls
        .blockingUnaryCall(_channel, hello2MethodDescriptor, getCallOptions, encode(m))
      resp.asInstanceOf[String]
    }
    def helloStreaming(name: String): Seq[String] = {
      val m                = Map("name" -> name)
      val responseObserver = GrpcClientCalls.blockingResponseObserver[String]
      ClientCalls.asyncServerStreamingCall(
        _channel.newCall(
          helloStreamingMethodDescriptor,
          getCallOptions
        ),
        encode(m),
        responseObserver
      )
      responseObserver.toRx.toSeq
    }
    def helloClientStreaming(input: Rx[String]): String = {
      val responseObserver = GrpcClientCalls.blockingResponseObserver[String]
      val requestObserver: ClientCallStreamObserver[MsgPack] = ClientCalls
        .asyncClientStreamingCall(
          _channel.newCall(
            helloClientStreamingMethodDescriptor,
            getCallOptions
          ),
          responseObserver
        ).asInstanceOf[ClientCallStreamObserver[MsgPack]]

      val c = GrpcClientCalls.readClientRequestStream(input, codecFactory.of[String], requestObserver, encoding)
      responseObserver.toRx.toSeq.head
    }

    def helloBidiStreaming(input: Rx[String]): Rx[String] = {
      val responseObserver = GrpcClientCalls.blockingResponseObserver[String]
      val requestObserver: ClientCallStreamObserver[MsgPack] = ClientCalls
        .asyncBidiStreamingCall(
          _channel.newCall(
            helloBidiStreamingMethodDescriptor,
            getCallOptions
          ),
          responseObserver
        ).asInstanceOf[ClientCallStreamObserver[MsgPack]]

      val c = GrpcClientCalls.readClientRequestStream(input, codecFactory.of[String], requestObserver, encoding)
      responseObserver.toRx
    }
    def helloOpt(opt: Option[String]): String = {
      val m = opt.map(x => Map("opt" -> opt)).getOrElse(Map.empty)
      val resp = ClientCalls
        .blockingUnaryCall(_channel, helloOptMethodDescriptor, getCallOptions, encode(m))
      resp.asInstanceOf[String]
    }

    def returnUnit(name: String): Unit = {
      val m = Map("name" -> name)
      val resp = ClientCalls
        .blockingUnaryCall(_channel, returnUnitMethodDescriptor, getCallOptions, encode(m))
      resp.asInstanceOf[Unit]
    }

    def error409Test: String = {
      val resp = ClientCalls
        .blockingUnaryCall(_channel, errorTestMethodDescriptor, getCallOptions, encode(Map.empty))

      resp.asInstanceOf[String]
    }

    def rpcExceptionTest(suppress: Boolean): String = {
      val resp = ClientCalls
        .blockingUnaryCall(
          _channel,
          rpcExceptionTestMethodDescriptor,
          getCallOptions,
          encode(Map("suppress" -> suppress))
        )

      resp.asInstanceOf[String]
    }
  }

}
