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

import scala.concurrent.Promise
import scala.util.Success

/**
  */
object GrpcClient {

  trait BlockingStreamObserver extends StreamObserver[Any] {
    def promise: Promise[Any]
  }

  private[grpc] def newSingleResponseObserver: BlockingStreamObserver =
    new BlockingStreamObserver {
      val promise: Promise[Any] = Promise[Any]()
      override def onNext(value: Any): Unit = {
        promise.complete(Success(value))
      }
      override def onError(t: Throwable): Unit = {
        promise.failure(t)
      }
      override def onCompleted(): Unit = {}
    }

}
