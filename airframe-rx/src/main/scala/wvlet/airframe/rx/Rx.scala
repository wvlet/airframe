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

/**
  *
  */
trait Rx[A] {
  import Rx._

  private[rx] def addDownstream[B](rx: Rx[B]): Rx[B]
  private[rx] def addSubscriber(s:Subscriber[A]): Unit

  def map[B](f: A => B): Rx[B] = MapOp[A, B](this, f))
  def flatMap[B](f: A => Rx[B]): Rx[B] = FlatMapOp(this, f)
  def withName(name:String): Rx[A] = NamedOp(this, name)

  def parents: Seq[Rx[_]]

  def subscribe[U](subscriber: A => U): Unit = {
    addSubscriber(Subscriber(subscriber))
    // Update downstream
    parents.map { p =>
      p.addDownstream(this)
    }
  }
}

object Rx {
  def of[A](v: A): Rx[A] = SingleOp(v)
  def variable[A](v: A): RxVar[A] = Rx.apply(v)
  def apply[A](v: A): RxVar[A] = new RxVar(v)

  private[rx] abstract class RxBase[A] extends Rx[A] {
    private[rx] var downStream: List[Rx[_]] = List.empty
    private[rx] override def addDownstream[B](rx: Rx[B]): Rx[B] = {
      synchronized {
        downStream = rx :: downStream
        rx
      }
    }
  }


  case class SingleOp[A](v:A) extends RxBase[A]
  case class MapOp[A, B](ref: Rx[A], f: A => B) extends RxBase[B]
  case class FlatMapOp[A, B](ref: Rx[A], f: A => Rx[B]) extends RxBase[B]
  case class NamedOp[A](ref: Rx[A], name:String) extends RxBase[A]

  class RxVar[A](private[rx] var currentValue: A) extends RxBase[A] {
    def :=(newValue: A): Unit = update(newValue)
    def update(newValue: A): Unit = {
      if (currentValue != newValue) {
        currentValue = newValue
        downStream.map { x =>
          //x.propagateUpdate()
        }
      }
    }
    //override private[rx] def propagateUpdate(x: A): Unit = {}
  }
}
