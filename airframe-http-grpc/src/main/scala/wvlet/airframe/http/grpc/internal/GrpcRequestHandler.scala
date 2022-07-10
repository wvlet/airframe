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
import io.grpc.stub.{ClientCallStreamObserver, ServerCallStreamObserver, StreamObserver}
import wvlet.airframe.codec.{MessageCodec, MessageCodecException, MessageCodecFactory}
import wvlet.airframe.http
import wvlet.airframe.http.RPCEncoding
import wvlet.airframe.http.grpc.{GrpcContext, GrpcResponse}
import wvlet.airframe.http.internal.RPCCallContext
import wvlet.airframe.http.router.HttpRequestMapper
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.msgpack.spi.Value.MapValue
import wvlet.airframe.surface.{CName, MethodSurface, Surface}
import wvlet.log.LogSupport
import wvlet.airframe.rx._

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Callable, ExecutorService}
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, ExecutionException, Promise}
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

  /**
    * Read the input value (MessagePack or Json) and convert to MessagePack Value
    *
    * @param grpcContext
    * @param request
    * @return
    */
  private def readRequestAsValue(grpcContext: Option[GrpcContext], request: MsgPack): MapValue = {
    try {
      val value = if (RPCEncoding.isJsonObjectMessage(request)) {
        // The input is a JSON message
        RPCEncoding.JSON.unpackValue(request)
      } else {
        // Check the message type using the accept header:
        val encoding = grpcContext.map(_.encoding).getOrElse(RPCEncoding.MsgPack)
        encoding.unpackValue(request)
      }

      value match {
        case m: MapValue =>
          m
        case _ =>
          val e = new IllegalArgumentException(s"Request data is not a MapValue: ${value}")
          reportError(e)
          requestLogger.logError(e, grpcContext, rpcContext)
          throw e
      }
    } catch {
      case e: MessageCodecException =>
        reportError(e)
        requestLogger.logError(e, grpcContext, rpcContext)
        throw e
    }
  }

  private def readStreamingInput[A](grpcContext: Option[GrpcContext], codec: MessageCodec[A], request: MsgPack): A = {
    val encoding = grpcContext.map(_.encoding).getOrElse(RPCEncoding.MsgPack)
    encoding match {
      case RPCEncoding.MsgPack =>
        codec.fromMsgPack(request)
      case RPCEncoding.JSON =>
        codec.fromJson(request)
    }
  }

  private def reportError(e: Throwable): Unit = {

    /**
      * Find the root cause of the exception from wrapped exception classes
      */
    @tailrec
    def findCause(e: Throwable): Throwable = {
      e match {
        case i: InvocationTargetException if i.getTargetException != null =>
          findCause(i.getTargetException)
        case ee: ExecutionException if ee.getCause != null =>
          findCause(ee.getCause)
        case _ =>
          e
      }
    }
    logger.error(findCause(e))
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
          if (arg.surface.isOption) {
            // If the argument is an option type, we can use None for the missing value
            None
          } else {
            val msg = s"No key for ${arg.name} is found in ${m} for calling ${methodSurface}"
            error(msg)
            throw new IllegalArgumentException(msg)
          }
        }
      }
      trace(s"RPC call ${methodSurface.name}(${args.mkString(", ")})")
      try {
        val ret = methodSurface.call(controller, args: _*)
        requestLogger.logRPC(grpcContext, rpcContext.withRPCArgs(args))
        ret
      } catch {
        case e: Throwable =>
          reportError(e)
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
    val encoding    = GrpcContext.currentEncoding
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
        Try(readStreamingInput(grpcContext, codec, value)) match {
          case Success(v) =>
            rx.add(OnNext(v))
          case Failure(e) =>
            reportError(e)
            rx.add(OnError(e))
        }
      }
      override def onError(t: Throwable): Unit = {
        reportError(t)
        requestLogger.logError(t, grpcContext, rpcContext)
        rx.add(OnError(t))
        responseObserver.onError(GrpcException.wrap(t))
      }
      override def onCompleted(): Unit = {
        invokeServerMethod
        rx.add(OnCompletion)
        promise.future.onComplete {
          case Success(value) =>
            requestLogger.logRPC(grpcContext, rpcContext)
            responseObserver.onNext(GrpcResponse(value, encoding))
            responseObserver.onCompleted()
          case Failure(e) =>
            reportError(e)
            requestLogger.logError(e, grpcContext, rpcContext)
            responseObserver.onError(GrpcException.wrap(e))
        }(ExecutionContext.fromExecutor(executorService))
      }
    }
    requestObserver
  }

  def invokeBidiStreamingMethod(
      responseObserver: StreamObserver[Any],
      clientStreamingType: Surface
  ): StreamObserver[MsgPack] = {

    val codec       = codecFactory.of(clientStreamingType)
    val grpcContext = GrpcContext.current
    val encoding    = GrpcContext.currentEncoding

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
        Try(readStreamingInput(grpcContext, codec, value)) match {
          case Success(v) =>
            // Add a log for each client-side stream message
            rx.add(OnNext(v))
          case Failure(e) =>
            rx.add(OnError(e))
        }
      }
      override def onError(t: Throwable): Unit = {
        reportError(t)
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
                    responseObserver.onNext(GrpcResponse(value, encoding))
                  case OnError(e) =>
                    reportError(e)
                    requestLogger.logError(e, grpcContext, rpcContext)
                    responseObserver.onError(GrpcException.wrap(e))
                  case OnCompletion =>
                    requestLogger.logRPC(grpcContext, rpcContext)
                    if (isReady(responseObserver)) {
                      responseObserver.onCompleted()
                    }
                }
              case other =>
                requestLogger.logRPC(grpcContext, rpcContext)
                responseObserver.onNext(GrpcResponse(v, encoding))
                responseObserver.onCompleted()
            }
          case Failure(e) =>
            reportError(e)
            requestLogger.logError(e, grpcContext, rpcContext)
            responseObserver.onError(GrpcException.wrap(e))
        }(ExecutionContext.fromExecutor(executorService))
      }
    }

    requestObserver
  }

  /**
    * Check whether the observer is ready for accepting more message
    * @param observer
    * @return
    */
  private def isReady(observer: StreamObserver[_]): Boolean = {
    observer match {
      case so: ServerCallStreamObserver[_] =>
        so.isReady
      case co: ClientCallStreamObserver[_] =>
        co.isReady
      case _ =>
        true
    }
  }

}

private[grpc] class RPCUnaryMethodHandler(rpcRequestHandler: GrpcRequestHandler)
    extends UnaryMethod[MsgPack, Any]
    with LogSupport {
  override def invoke(
      request: MsgPack,
      responseObserver: StreamObserver[Any]
  ): Unit = {
    val encoding = GrpcContext.currentEncoding
    rpcRequestHandler.invokeMethod(request) match {
      case Success(v) =>
        responseObserver.onNext(GrpcResponse(v, encoding))
        responseObserver.onCompleted()
      case Failure(e) =>
        responseObserver.onError(GrpcException.wrap(e))
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
    val encoding = GrpcContext.currentEncoding
    rpcRequestHandler.invokeMethod(request) match {
      case Success(v) =>
        v match {
          case rx: Rx[_] =>
            var c = Cancelable.empty
            c = RxRunner.run(rx) {
              case OnNext(value) =>
                responseObserver.onNext(GrpcResponse(value, encoding))
              case OnError(e) =>
                responseObserver.onError(GrpcException.wrap(e))
              case OnCompletion =>
                responseObserver.onCompleted()
            }
          case other =>
            responseObserver.onNext(GrpcResponse(v, encoding))
            responseObserver.onCompleted()
        }
      case Failure(e) =>
        responseObserver.onError(GrpcException.wrap(e))
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
