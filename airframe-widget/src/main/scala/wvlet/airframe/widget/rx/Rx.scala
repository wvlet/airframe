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
package wvlet.airframe.widget.rx

/**
  *
  */
sealed trait Rx[A] {
  import Rx._

  private[rx] def addDownStream[B](rx: Rx[B]): Rx[B]

  private[rx] def propagateUpdate(x: A): Unit = {}

  def map[B](f: A => B): Rx[B] = {
    addDownStream(MapOp[A, B](this, f))
  }
  def flatMap[B](f: A => Rx[B]): Rx[B] = {
    addDownStream(FlatMapOp(this, f))
  }

  def register[Leaf](actionNode: Rx[Leaf]): Unit = {
    this match {
      case rootVar: RxVar[_] =>
        rootVar.addDownStream(actionNode)
      case MapOp(self, f) =>
        self.register(actionNode)

    }
  }
}

object Rx {

  def apply[A](v: A): RxVar[A] = new RxVar(v)

  abstract class RxBase[A] extends Rx[A] {
    private[rx] var downStream: List[Rx[_]] = List.empty
    private[rx] override def addDownStream[B](rx: Rx[B]): Rx[B] = {
      downStream = rx :: downStream
      rx
    }
  }

  class RxVar[A](initialValue: A) extends RxBase[A] {
    private[rx] var currentValue: A = initialValue

    def update(newValue: A): Unit = {
      if (currentValue != newValue) {
        currentValue = newValue
        downStream.map { x =>
          x.propagateUpdate()
        }
      }
    }
    override private[rx] def propagateUpdate(x: A): Unit = {}
  }

  case class MapOp[A, B](ref: Rx[A], f: A => B) extends RxBase[B] {}

  case class FlatMapOp[A, B](ref: Rx[A], f: A => Rx[B]) extends RxBase[B]

  def process[A](rx: Rx[A], v: A): Unit = {
    rx match {
      case m @ MapOp(ref, f) =>
        process(m, f.asInstanceOf[A => _](v))
      case fm @ FlatMapOp(ref, f) =>
        process(fm, f.asInstanceOf)
    }
  }
}
