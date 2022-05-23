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

import io.grpc.{CallOptions, Status, StatusException, StatusRuntimeException}
import io.grpc.stub.{ClientCalls, StreamObserver}
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.grpc.internal.GrpcException
import wvlet.airframe.http.{RPCEncoding, RPCException, RPCStatus}
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.rx.{Cancelable, OnCompletion, OnError, OnNext, Rx, RxBlockingQueue, RxRunner, RxStream}
import wvlet.log.LogSupport

import scala.util.{Failure, Success, Try}

case class GrpcClientConfig(
    rpcEncoding: RPCEncoding = RPCEncoding.MsgPack,
    callOptions: CallOptions
)

case class GrpcMethod[Req, Resp](
    /**
      * Grpc method descriptor, which contains a response encoder of MessageCodec[Resp]
      */
    descriptor: io.grpc.MethodDescriptor[Array[Byte], Resp],
    requestCodec: MessageCodec[Req]
)

class GrpcClient(channel: io.grpc.Channel, config: GrpcClientConfig) {
  import GrpcClient._

  private def prepareRPCRequestBody[Req, Resp](method: GrpcMethod[Req, Resp], request: Req): Array[Byte] = {
    try {
      config.rpcEncoding.encodeWithCodec(request, method.requestCodec)
    } catch {
      case e: Throwable =>
        throw RPCStatus.INVALID_ARGUMENT_U2.newException(
          message = s"Failed to encode the RPC request argument: ${request}: ${e.getMessage}",
          cause = e
        )
    }
  }

  protected def getChannel: io.grpc.Channel = {
    channel
  }

  def unaryCall[Req, Resp](
      method: GrpcMethod[Req, Resp],
      request: Req
  ): Resp = {
    val requestBody: Array[Byte] = prepareRPCRequestBody(method, request)
    try {
      ClientCalls
        .blockingUnaryCall[Array[Byte], Resp](getChannel, method.descriptor, config.callOptions, requestBody)
        .asInstanceOf[Resp]
    } catch {
      case e: StatusRuntimeException =>
        throw translateException(e)
    }
  }

  def serverStreamingCall[Req, Resp](
      method: GrpcMethod[Req, Resp],
      request: Req
  ): RxStream[Resp] = {
    val requestBody: Array[Byte] = prepareRPCRequestBody(method, request)
    val responseObserver         = new RxStreamObserver[Resp]
    ClientCalls.asyncServerStreamingCall[MsgPack, Resp](
      getChannel.newCall(method.descriptor, config.callOptions),
      requestBody,
      new GrpcStreamObserverWrapper(responseObserver)
    )
    responseObserver.toRx
  }

  def clientStreamingCall[Req, Resp](
      method: GrpcMethod[Req, Resp],
      request: RxStream[Req]
  ): Resp = {
    val responseObserver = GrpcClient.blockingRxResponseObserver[Resp]
    val requestObserver = ClientCalls.asyncClientStreamingCall(
      getChannel.newCall(method.descriptor, config.callOptions),
      new GrpcStreamObserverWrapper[Resp](responseObserver)
    )
    GrpcClient.readClientRequestStream(
      request,
      method.requestCodec,
      requestObserver,
      encoding = config.rpcEncoding
    )
    responseObserver.toRx.toSeq.head
  }

  def asyncClientStreamingCall[Req, Resp](
      method: GrpcMethod[Req, Resp],
      responseObserver: StreamObserver[Resp]
  ): StreamObserver[Req] = {
    val requestObserver = ClientCalls.asyncClientStreamingCall(
      getChannel.newCall(method.descriptor, config.callOptions),
      new GrpcStreamObserverWrapper[Resp](responseObserver)
    )
    // Return a StreamObserver for receiving a stream of Req objects from the client
    GrpcClient.wrapRequestObserver[MsgPack, Req](
      requestObserver,
      config.rpcEncoding.encodeWithCodec(_, method.requestCodec)
    )
  }

  def asyncUnaryCall[Req, Resp](
      method: GrpcMethod[Req, Resp],
      request: Req,
      responseObserver: StreamObserver[Resp]
  ): Unit = {
    try {
      val requestBody: Array[Byte] = prepareRPCRequestBody(method, request)
      ClientCalls.asyncUnaryCall(
        getChannel.newCall(method.descriptor, config.callOptions),
        requestBody,
        new GrpcStreamObserverWrapper(responseObserver)
      )
    } catch {
      case e: Throwable =>
        responseObserver.onError(translateException(e))
    }
  }

  def asyncServerStreamingCall[Req, Resp](
      method: GrpcMethod[Req, Resp],
      request: Req,
      responseObserver: StreamObserver[Resp]
  ): Unit = {
    try {
      val requestBody = prepareRPCRequestBody(method, request)
      ClientCalls.asyncServerStreamingCall(
        getChannel.newCall(method.descriptor, config.callOptions),
        requestBody,
        new GrpcStreamObserverWrapper(responseObserver)
      )
    } catch {
      case e: Throwable =>
        responseObserver.onError(translateException(e))
    }

  }
}

object GrpcClient extends LogSupport {

  trait BlockingRxStreamObserver[A] extends StreamObserver[A] {
    def toRx: RxStream[A]
  }

  private def blockingRxResponseObserver[A]: BlockingRxStreamObserver[A] =
    new BlockingRxStreamObserver[A] {
      val toRx: RxBlockingQueue[A] = new RxBlockingQueue[A]
      override def onNext(v: A): Unit = {
        toRx.add(OnNext(v))
      }
      override def onError(t: Throwable): Unit = {
        toRx.add(OnError(t))
      }
      override def onCompleted(): Unit = {
        toRx.add(OnCompletion)
      }
    }

  private class RxStreamObserver[A] extends StreamObserver[A] {
    val toRx: RxBlockingQueue[A] = new RxBlockingQueue[A]
    override def onNext(v: A): Unit = {
      toRx.add(OnNext(v))
    }
    override def onError(t: Throwable): Unit = {
      toRx.add(OnError(t))
    }
    override def onCompleted(): Unit = {
      toRx.add(OnCompletion)
    }
  }

  /**
    * Translate exception to RPCException
    * @param e
    * @return
    */
  private def translateException(e: Throwable): Throwable = {
    e match {
      case e: RPCException => e
      case ex: StatusRuntimeException =>
        try {
          val trailers = Status.trailersFromThrowable(ex)
          // For the server-side RPC error, it should have an RPCException message in the trailer
          if (trailers != null && trailers.containsKey(GrpcException.rpcErrorBodyKey)) {
            try {
              val rpcErrorJson = trailers.get[String](GrpcException.rpcErrorBodyKey)
              RPCException.fromJson(rpcErrorJson)
            } catch {
              case e: Throwable =>
                RPCStatus.DATA_LOSS_I8.newException(s"Failed to parse the RPC error details: ${ex.getMessage}", e)
            }
          } else {
            // Other gRPC errors
            val rpcStatus = RPCStatus.fromGrpcStatusCode(ex.getStatus.getCode.value())
            rpcStatus.newException(s"gRPC failure: ${ex.getMessage}", ex)
          }
        } catch {
          case e: Throwable =>
            warn(s"Failed to translate to RPCException", e)
            // Return the original exception
            ex
        }
      case other =>
        other
    }
  }

  /**
    * Wrapper of the user provided StreamObserver to properly report RPCException message embedded in the trailer of
    * StatusRuntimeException
    */
  private class GrpcStreamObserverWrapper[Resp](observer: io.grpc.stub.StreamObserver[Resp])
      extends StreamObserver[Resp] {
    override def onNext(value: Resp): Unit = {
      observer.onNext(value)
    }

    override def onError(t: Throwable): Unit = {
      observer.onError(translateException(t))
    }

    override def onCompleted(): Unit = {
      observer.onCompleted()
    }
  }

  private def readClientRequestStream[A](
      input: Rx[A],
      codec: MessageCodec[A],
      requestObserver: StreamObserver[MsgPack],
      encoding: RPCEncoding = RPCEncoding.MsgPack
  ): Cancelable = {
    RxRunner.run(input) {
      case OnNext(x) => {
        Try(encoding.encodeWithCodec(x.asInstanceOf[A], codec)) match {
          case Success(msgpack) =>
            requestObserver.onNext(msgpack)
          case Failure(e) =>
            requestObserver.onError(e)
        }
      }
      case OnError(e) => requestObserver.onError(e)
      case OnCompletion => {
        requestObserver.onCompleted()
      }
    }
  }

  /**
    * Wrap the client-side StreamObserver
    * @param observer
    * @param f
    * @tparam A
    * @tparam B
    * @return
    */
  private def wrapRequestObserver[A, B](observer: StreamObserver[A], f: B => A): StreamObserver[B] = {
    new StreamObserver[B] {
      override def onNext(value: B): Unit = {
        Try(f(value)) match {
          case Success(a) => observer.onNext(a)
          case Failure(e) =>
            observer.onError(
              RPCStatus.INVALID_ARGUMENT_U2
                .newException(s"Failed to encode the request value ${value}: ${e.getMessage}", e)
            )
        }
      }
      override def onError(t: Throwable): Unit = {
        observer.onError(translateException(t))
      }
      override def onCompleted(): Unit = {
        observer.onCompleted()
      }
    }
  }
}
