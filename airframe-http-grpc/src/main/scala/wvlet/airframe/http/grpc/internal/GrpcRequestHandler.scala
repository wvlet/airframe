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

import io.grpc.stub.ServerCalls.{BidiStreamingMethod, ClientStreamingMethod, ServerStreamingMethod, UnaryMethod}
import io.grpc.stub.StreamObserver
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.grpc.{GrpcContext, GrpcEncoding}
import wvlet.airframe.http.router.{HttpRequestMapper, RPCCallContext}
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.msgpack.spi.Value.MapValue
import wvlet.airframe.surface.{CName, MethodSurface, Surface}
import wvlet.log.LogSupport
import wvlet.airframe.rx._

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Callable, ExecutorService}
import scala.concurrent.{ExecutionContext, Promise}
import scala.util.{Failure, Success, Try}

/**
  * This handler receives a MessagePack Map value for an RPC request, and call the corresponding controller method
  */
class GrpcRequestHandler(
    rpcInterfaceCls: Class[_],
    // Controller instance
    controller: Any,
    // Controller method to call for RPC
    methodSurface: MethodSurface,
    codecFactory: MessageCodecFactory,
    executorService: ExecutorService,
    requestLogger: GrpcRequestLogger
) extends LogSupport {

  private val argCodecs = methodSurface.args.map(a => codecFactory.of(a.surface))

  private val rpcContext = RPCCallContext(rpcInterfaceCls, methodSurface, Seq.empty)

  private def isJsonObjectMessage(request: MsgPack): Boolean = {
    request.size > 0 && request.head == '{' && request.last == '}'
  }

  /**
    * Read the input value (MessagePack or Json) and convert to MessagePack Value
    *
    * @param grpcContext
    * @param request
    * @return
    */
  def readRequestAsValue(grpcContext: Option[GrpcContext], request: MsgPack): MapValue = {
    val value = if (isJsonObjectMessage(request)) {
      // The input is a JSON message
      GrpcEncoding.JSON.unpackValue(request)
    } else {
      // Check the message type using content-type header:
      val contentType = grpcContext.map(_.accept)
      contentType match {
        case Some(GrpcEncoding.ContentTypeJson) =>
          // Json input
          GrpcEncoding.MsgPack.unpackValue(request)
        case _ =>
          // Use msgpack by default
          GrpcEncoding.MsgPack.unpackValue(request)
      }
    }

    value match {
      case m: MapValue =>
        m
      case _ =>
        val e = new IllegalArgumentException(s"Invalid argument: ${value}")
        requestLogger.logError(e, grpcContext, rpcContext)
        throw e
    }
  }

  def invokeMethod(request: MsgPack): Try[Any] = {
    val grpcContext = GrpcContext.current

    // Build method arguments from MsgPack

    val result = Try {
      val m: MapValue = readRequestAsValue(grpcContext, request)
      val mapValue    = HttpRequestMapper.toCanonicalKeyNameMap(m)
      val args = for ((arg, i) <- methodSurface.args.zipWithIndex) yield {
        val argOpt = mapValue.get(CName.toCanonicalName(arg.name)) match {
          case Some(paramValue) =>
            Option(argCodecs(i).fromMsgPack(paramValue.toMsgpack)).orElse {
              throw new IllegalArgumentException(s"Failed to parse ${paramValue} for ${arg}")
            }
          case None =>
            // If no value is found, use the method parameter's default argument
            arg.getMethodArgDefaultValue(controller)
        }
        argOpt.getOrElse {
          throw new IllegalArgumentException(s"No key for ${arg.name} is found in ${m}")
        }
      }
      trace(s"RPC call ${methodSurface.name}(${args.mkString(", ")})")
      try {
        val ret = methodSurface.call(controller, args: _*)
        requestLogger.logRPC(grpcContext, rpcContext.withRPCArgs(args))
        ret
      } catch {
        case e: Throwable =>
          requestLogger.logError(e, grpcContext, rpcContext.withRPCArgs(args))
          throw e
      }
    }
    result
  }

  def invokeClientStreamingMethod(
      responseObserver: StreamObserver[Any],
      clientStreamingType: Surface
  ): StreamObserver[MsgPack] = {
    val codec = codecFactory.of(clientStreamingType)
    // An observer that receives streaming messages from the client
    val grpcContext = GrpcContext.current
    val requestObserver = new StreamObserver[MsgPack] {
      private val isStarted = new AtomicBoolean(false)
      // A queue for passing incoming messages to the application through Rx interface
      private val rx                    = new RxBlockingQueue[Any]
      private val promise: Promise[Any] = Promise()

      private def invokeServerMethod: Unit = {
        // Avoid duplicated execution
        if (isStarted.compareAndSet(false, true)) {
          // We need to run the server RPC method in another thread as rx implementation is blocking
          executorService.submit(new Callable[Unit] {
            override def call(): Unit = {
              Try(methodSurface.call(controller, rx)) match {
                case Success(value) =>
                  promise.success(value)
                case Failure(e) =>
                  promise.failure(e)
              }
            }
          })
        }
      }

      override def onNext(value: MsgPack): Unit = {
        invokeServerMethod
        Try(codec.fromMsgPack(value)) match {
          case Success(v) =>
            rx.add(OnNext(v))
          case Failure(e) =>
            rx.add(OnError(e))
        }
      }
      override def onError(t: Throwable): Unit = {
        requestLogger.logError(t, grpcContext, rpcContext)
        rx.add(OnError(t))
        responseObserver.onError(t)
      }
      override def onCompleted(): Unit = {
        invokeServerMethod
        rx.add(OnCompletion)
        promise.future.onComplete {
          case Success(value) =>
            requestLogger.logRPC(grpcContext, rpcContext)
            responseObserver.onNext(value)
            responseObserver.onCompleted()
          case Failure(e) =>
            requestLogger.logError(e, grpcContext, rpcContext)
            responseObserver.onError(e)
        }(ExecutionContext.fromExecutor(executorService))
      }
    }
    requestObserver
  }

  def invokeBidiStreamingMethod(
      responseObserver: StreamObserver[Any],
      clientStreamingType: Surface
  ): StreamObserver[MsgPack] = {

    val codec = codecFactory.of(clientStreamingType)

    val grpcContext = GrpcContext.current
    val requestObserver = new StreamObserver[MsgPack] {
      private val isStarted             = new AtomicBoolean(false)
      private val rx                    = new RxBlockingQueue[Any]
      private val promise: Promise[Any] = Promise()

      private def invokeServerMethod: Unit = {
        // Avoid duplicated execution
        if (isStarted.compareAndSet(false, true)) {
          // We need to run the server RPC method in another thread as rx implementation is blocking
          executorService.submit(new Callable[Unit] {
            override def call(): Unit = {
              Try(methodSurface.call(controller, rx)) match {
                case Success(value) => promise.success(value)
                case Failure(e)     => promise.failure(e)
              }
            }
          })
        }
      }

      override def onNext(value: MsgPack): Unit = {
        invokeServerMethod
        Try(codec.fromMsgPack(value)) match {
          case Success(v) =>
            // Add a log for each client-side stream message
            rx.add(OnNext(v))
          case Failure(e) =>
            rx.add(OnError(e))
        }
      }
      override def onError(t: Throwable): Unit = {
        rx.add(OnError(t))
        responseObserver.onError(t)
      }
      override def onCompleted(): Unit = {
        invokeServerMethod
        rx.add(OnCompletion)
        promise.future.onComplete {
          case Success(v) =>
            v match {
              case rx: Rx[_] =>
                var c = Cancelable.empty
                c = RxRunner.run(rx) {
                  case OnNext(value) =>
                    responseObserver.onNext(value)
                  case OnError(e) =>
                    requestLogger.logError(e, grpcContext, rpcContext)
                    responseObserver.onError(e)
                  case OnCompletion =>
                    requestLogger.logRPC(grpcContext, rpcContext)
                    responseObserver.onCompleted()
                }
              case other =>
                requestLogger.logRPC(grpcContext, rpcContext)
                responseObserver.onNext(v)
                responseObserver.onCompleted()
            }
          case Failure(e) =>
            requestLogger.logError(e, grpcContext, rpcContext)
            responseObserver.onError(e)
        }(ExecutionContext.fromExecutor(executorService))
      }
    }

    requestObserver
  }

}

private[grpc] class RPCUnaryMethodHandler(rpcRequestHandler: GrpcRequestHandler) extends UnaryMethod[MsgPack, Any] {
  override def invoke(
      request: MsgPack,
      responseObserver: StreamObserver[Any]
  ): Unit = {
    rpcRequestHandler.invokeMethod(request) match {
      case Success(v) =>
        responseObserver.onNext(v)
        responseObserver.onCompleted()
      case Failure(e) =>
        responseObserver.onError(e)
    }
  }
}

private[grpc] class RPCServerStreamingMethodHandler(rpcRequestHandler: GrpcRequestHandler)
    extends ServerStreamingMethod[MsgPack, Any]
    with LogSupport {
  override def invoke(
      request: MsgPack,
      responseObserver: StreamObserver[Any]
  ): Unit = {
    rpcRequestHandler.invokeMethod(request) match {
      case Success(v) =>
        v match {
          case rx: Rx[_] =>
            var c = Cancelable.empty
            c = RxRunner.run(rx) {
              case OnNext(value) =>
                responseObserver.onNext(value)
              case OnError(e) =>
                responseObserver.onError(e)
              case OnCompletion =>
                responseObserver.onCompleted()
            }
          case other =>
            responseObserver.onNext(v)
            responseObserver.onCompleted()
        }
      case Failure(e) =>
        responseObserver.onError(e)
    }
  }
}

private[grpc] class RPCClientStreamingMethodHandler(rpcRequestHandler: GrpcRequestHandler, clientStreamingType: Surface)
    extends ClientStreamingMethod[MsgPack, Any] {

  override def invoke(
      responseObserver: StreamObserver[Any]
  ): StreamObserver[MsgPack] = {
    val requestObserver = rpcRequestHandler.invokeClientStreamingMethod(responseObserver, clientStreamingType)
    requestObserver
  }
}

private[grpc] class RPCBidiStreamingMethodHandler(rpcRequestHandler: GrpcRequestHandler, clientStreamingType: Surface)
    extends BidiStreamingMethod[MsgPack, Any] {
  override def invoke(
      responseObserver: StreamObserver[Any]
  ): StreamObserver[MsgPack] = {
    val requestObserver = rpcRequestHandler.invokeBidiStreamingMethod(responseObserver, clientStreamingType)
    requestObserver
  }
}
