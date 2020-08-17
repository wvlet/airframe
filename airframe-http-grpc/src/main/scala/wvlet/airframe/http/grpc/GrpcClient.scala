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
import wvlet.airframe.rx.{OnCompletion, OnError, OnNext, Rx, RxEvent, RxVar}

import scala.collection.immutable.Queue

/**
  */
object GrpcClient {

  class RxObserver[A] extends StreamObserver[A] {
    private var eventQueue = Queue.empty[RxEvent]

    def toRx: Rx[A] = ???

    override def onNext(value: A): Unit = {
      eventQueue = eventQueue.enqueue(OnNext(value))
    }
    override def onError(t: Throwable): Unit = {
      eventQueue = eventQueue.enqueue(OnError(t))
    }
    override def onCompleted(): Unit = {
      eventQueue = eventQueue.enqueue(OnCompletion)
    }
  }
}
