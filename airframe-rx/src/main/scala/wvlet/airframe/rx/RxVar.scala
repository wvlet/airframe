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

import scala.collection.mutable.ArrayBuffer
import scala.language.higherKinds
import scala.util.Try

/**
  * A reactive variable supporting update and propagation of the updated value to the chained operators
  */
class RxVar[A](private var currentValue: A) extends RxStream[A] with RxVarOps[A] {
  override def toString: String                        = s"RxVar(${currentValue})"
  override def parents: Seq[Rx[_]]                     = Seq.empty
  private var subscribers: ArrayBuffer[RxEvent => Any] = ArrayBuffer.empty

  override def toOption[X, A1 >: A](implicit ev: A1 <:< Option[X]): RxOptionVar[X] =
    new RxOptionVar(this.asInstanceOf[RxVar[Option[X]]])

  override def get: A = currentValue

  override def foreach[U](f: A => U): Cancelable = {
    val s = { ev: RxEvent =>
      ev match {
        case OnNext(v) =>
          f(v.asInstanceOf[A])
        case OnError(e) =>
          throw e
        case _ =>
      }
    }
    foreachEvent(s)
  }

  override def foreachEvent[U](effect: RxEvent => U): Cancelable = {
    // Register a subscriber for propagating future changes
    subscribers += effect
    effect(OnNext(currentValue))
    Cancelable { () =>
      // Unsubscribe if cancelled
      subscribers -= effect
    }
  }

  /**
    * Updates the variable and trigger the recalculation of the subscribers
    * currentValue => newValue
    */
  override def update(updater: A => A, force: Boolean = false): Unit = {
    val newValue = updater(currentValue)
    if (force || currentValue != newValue) {
      currentValue = newValue
      propagateEvent(OnNext(newValue))
    }
  }

  private def propagateEvent(e: RxEvent): Unit = {
    subscribers.foreach { s =>
      // The subscriber instance might be null if it is cleaned up in JS
      Option(s).foreach(subscriber => Try(subscriber(e)))
    }
  }

  override def setException(e: Throwable): Unit = {
    propagateEvent(OnError(e))
  }
}

object RxVar {}

trait RxVarOps[A] {
  def get: A
  def foreach[U](f: A => U): Cancelable
  def :=(newValue: A): Unit = set(newValue)
  def set(newValue: A): Unit =
    update { x: A =>
      newValue
    }
  def forceSet(newValue: A): Unit =
    update(
      { x: A =>
        newValue
      },
      force = true
    )

  /**
    * Update the variable and force notification to subscribers
    * @param updater
    */
  def forceUpdate(updater: A => A): Unit = update(updater, force = true)

  /**
    * Updates the variable and trigger the recalculation of the subscribers
    * currentValue => newValue
    */
  def update(updater: A => A, force: Boolean = false): Unit

  /**
    * Propagate an error to the subscribers
    */
  def setException(e: Throwable): Unit

  def foreachEvent[U](effect: RxEvent => U): Cancelable

}
