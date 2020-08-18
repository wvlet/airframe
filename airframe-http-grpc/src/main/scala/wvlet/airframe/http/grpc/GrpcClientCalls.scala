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
import io.grpc.stub.StreamObserver
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.rx.{Cancelable, OnCompletion, OnError, OnNext, Rx, RxBlockingQueue, RxRunner}
import wvlet.log.LogSupport

import scala.util.{Failure, Success, Try}

/**
  * Helper methods for making gRPC calls
  */
object GrpcClientCalls extends LogSupport {

  trait BlockingStreamObserver[A] extends StreamObserver[Any] {
    def toRx: Rx[A]
  }

  def blockingResponseObserver[A]: BlockingStreamObserver[A] =
    new BlockingStreamObserver[A] {
      val toRx: RxBlockingQueue[A] = new RxBlockingQueue[A]
      override def onNext(value: Any): Unit = {
        toRx.add(OnNext(value))
      }
      override def onError(t: Throwable): Unit = {
        toRx.add(OnError(t))
      }
      override def onCompleted(): Unit = {
        toRx.add(OnCompletion)
      }
    }

  def readClientRequestStream[A](
      input: Rx[A],
      codec: MessageCodec[A],
      requestObserver: StreamObserver[MsgPack]
  ): Cancelable = {
    RxRunner.run(input) {
      case OnNext(x) => {
        Try(codec.toMsgPack(x.asInstanceOf[A])) match {
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

  def translate[A, B](observer: StreamObserver[A], f: B => A): StreamObserver[B] =
    new StreamObserver[B] {
      override def onNext(value: B): Unit = {
        Try(f(value)) match {
          case Success(a) => observer.onNext(a)
          case Failure(e) => observer.onError(e)
        }
      }
      override def onError(t: Throwable): Unit = {
        observer.onError(t)
      }
      override def onCompleted(): Unit = {
        observer.onCompleted()
      }
    }
}
