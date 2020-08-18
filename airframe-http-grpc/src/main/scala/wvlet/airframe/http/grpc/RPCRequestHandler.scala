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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Callable, ExecutorService}

import io.grpc.stub.ServerCalls.{BidiStreamingMethod, ClientStreamingMethod, ServerStreamingMethod, UnaryMethod}
import io.grpc.stub.StreamObserver
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.codec.PrimitiveCodec.ValueCodec
import wvlet.airframe.http.router.HttpRequestMapper
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.msgpack.spi.Value.MapValue
import wvlet.airframe.rx.{Cancelable, OnCompletion, OnError, OnNext, Rx, RxBlockingQueue, RxRunner}
import wvlet.airframe.surface.{CName, MethodSurface, Surface}
import wvlet.log.LogSupport

import scala.concurrent.{ExecutionContext, Promise}
import scala.util.{Failure, Success, Try}

/**
  * Receives MessagePack Map value for the RPC request, and call the controller method
  */
class RPCRequestHandler(
    controller: Any,
    methodSurface: MethodSurface,
    codecFactory: MessageCodecFactory,
    executorService: ExecutorService
) extends LogSupport {

  private val argCodecs = methodSurface.args.map(a => codecFactory.of(a.surface))

  def invokeMethod(request: MsgPack): Try[Any] = {
    // Build method arguments from MsgPack
    val requestValue = ValueCodec.unpack(request)
    trace(requestValue)

    val result = Try {
      requestValue match {
        case m: MapValue =>
          val mapValue = HttpRequestMapper.toCanonicalKeyNameMap(m)
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
          methodSurface.call(controller, args: _*)
        case _ =>
          throw new IllegalArgumentException(s"Invalid argument: ${requestValue}")
      }
    }
    result
  }

  def invokeClientStreamingMethod(
      responseObserver: StreamObserver[Any],
      clientStreamingType: Surface
  ): StreamObserver[MsgPack] = {
    val codec = codecFactory.of(clientStreamingType)
    // Receives streaming messages from the client
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
            responseObserver.onNext(v)
            responseObserver.onCompleted()
          case Failure(e) =>
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
          case Success(v) => rx.add(OnNext(v))
          case Failure(e) => rx.add(OnError(e))
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
        }(ExecutionContext.fromExecutor(executorService))
      }
    }

    requestObserver
  }

}

class RPCUnaryMethodHandler(rpcRequestHandler: RPCRequestHandler) extends UnaryMethod[MsgPack, Any] {
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

class RPCServerStreamingMethodHandler(rpcRequestHandler: RPCRequestHandler)
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

class RPCClientStreamingMethodHandler(rpcRequestHandler: RPCRequestHandler, clientStreamingType: Surface)
    extends ClientStreamingMethod[MsgPack, Any] {

  override def invoke(
      responseObserver: StreamObserver[Any]
  ): StreamObserver[MsgPack] = {
    val requestObserver = rpcRequestHandler.invokeClientStreamingMethod(responseObserver, clientStreamingType)
    requestObserver
  }
}

class RPCBidiStreamingMethodHandler(rpcRequestHandler: RPCRequestHandler, clientStreamingType: Surface)
    extends BidiStreamingMethod[MsgPack, Any] {
  override def invoke(
      responseObserver: StreamObserver[Any]
  ): StreamObserver[MsgPack] = {
    val requestObserver = rpcRequestHandler.invokeBidiStreamingMethod(responseObserver, clientStreamingType)
    requestObserver
  }
}
