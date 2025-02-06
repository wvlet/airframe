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
package wvlet.airframe.rx

import scala.concurrent.{ExecutionContext, Promise}

/**
  * JVM/JS compatible RxQueue implementation
  * @tparam A
  */
class RxQueue[A] extends RxSource[A] {
  private implicit val ec: ExecutionContext = compat.defaultExecutionContext
  override def parents: Seq[Rx[_]]          = Seq.empty

  private var queue                             = scala.collection.immutable.Queue.empty[RxEvent]
  private var waiting: Option[Promise[RxEvent]] = None

  override def add(event: RxEvent): Unit = {
    synchronized {
      queue = queue.enqueue(event)
      waiting match {
        case Some(p) =>
          if (queue.nonEmpty) {
            waiting = None
            val (e, newQueue) = queue.dequeue
            queue = newQueue
            p.success(e)
          }
        case None =>
        // do nothing if there is no waiting promise
      }
    }
  }

  override def next: Rx[RxEvent] = {
    synchronized {
      if (queue.nonEmpty) {
        val (e, newQueue) = queue.dequeue
        queue = newQueue
        Rx.const(e)
      } else {
        val p = Promise[RxEvent]()
        waiting = Some(p)
        Rx.future(p.future)
      }
    }
  }
}
