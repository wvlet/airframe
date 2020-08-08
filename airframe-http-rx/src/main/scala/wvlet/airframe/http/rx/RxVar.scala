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
package wvlet.airframe.http.rx
import wvlet.airframe.http.rx.Rx.RxBase

import scala.collection.mutable.ArrayBuffer

/**
  * A reactive variable supporting update and propagation of the updated value to the chained operators
  */
class RxVar[A](protected var currentValue: A) extends RxBase[A] {
  override def toString: String    = s"RxVar(${currentValue})"
  override def parents: Seq[Rx[_]] = Seq.empty

  private var subscribers: ArrayBuffer[Subscriber[A]] = ArrayBuffer.empty

  def get: A = currentValue
  def foreach[U](f: A => U): Cancelable = {
    val s = Subscriber(f)
    // Register a subscriber for propagating future changes
    subscribers += s
    f(currentValue)
    Cancelable { () =>
      // Unsubscribe if cancelled
      subscribers -= s
    }
  }

  def :=(newValue: A): Unit  = set(newValue)
  def set(newValue: A): Unit = update { x: A => newValue }

  def forceSet(newValue: A): Unit = update({ x: A => newValue }, force = true)

  /**
    * Updates the variable and trigger the recalculation of the subscribers
    * currentValue => newValue
    */
  def update(updater: A => A, force: Boolean = false): Unit = {
    val newValue = updater(currentValue)
    if (force || currentValue != newValue) {
      currentValue = newValue
      subscribers.map { s =>
        // The subscriber instance might be null if it is cleaned up in JS
        Option(s).foreach(_.apply(newValue))
      }
    }
  }

  /**
    * Update the variable and force notification to subscribers
    * @param updater
    */
  def forceUpdate(updater: A => A): Unit = update(updater, force = true)
}
