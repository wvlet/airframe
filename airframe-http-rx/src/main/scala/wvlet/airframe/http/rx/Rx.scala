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

import wvlet.log.LogSupport

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

/**
  */
trait Rx[A] extends LogSupport {
  import Rx._

  def map[B](f: A => B): Rx[B]           = MapOp[A, B](this, f)
  def flatMap[B](f: A => Rx[B]): Rx[B]   = FlatMapOp(this, f)
  def filter(f: A => Boolean): Rx[A]     = FilterOp(this, f)
  def withFilter(f: A => Boolean): Rx[A] = FilterOp(this, f)

  /**
    * Combine two Rx objects to form a pair. If one of the objects is updated,
    * it will yield a new pair.
    *
    * This method is useful when you need to monitor multiple Rx objects.
    *
    * Using Zip will be more intuitive than nesting multiple Rx operators
    * like Rx[A].map { x => ... Rx[B].map { ...} }.
    */
  def zip[B](other: Rx[B]): Rx[(A, B)]             = ZipOp(this, other)
  def zip[B, C](b: Rx[B], c: Rx[C]): Rx[(A, B, C)] = Zip3Op(this, b, c)

  def withName(name: String): Rx[A] = NamedOp(this, name)

  def parents: Seq[Rx[_]]

  /**
    * Subscribe any change in the upstream, and if a change is detected,
    *  the given subscriber code will be executed.
    *
    * @param subscriber
    * @tparam U
    * @return
    */
  def subscribe[U](subscriber: A => U): Cancelable = {
    Rx.run(this)(subscriber)
  }
  def run(effect: A => Unit): Cancelable = Rx.run(this)(effect)

}

object Rx extends LogSupport {
  def const[A](v: A): Rx[A]       = SingleOp(v)
  def variable[A](v: A): RxVar[A] = Rx.apply(v)
  def apply[A](v: A): RxVar[A]    = new RxVar(v)

  /**
    * Mapping a Scala Future into Rx
    * @param f
    * @param ec
    * @tparam A
    * @return
    */
  def fromFuture[A](f: Future[A])(implicit ec: ExecutionContext): Rx[Option[A]] = {
    val v = Rx.variable[Option[A]](None)
    f.foreach { x => v := Some(x) }
    v
  }

  /**
    * Build a executable chain of Rx operators, and the resulting chain
    * will be registered to the root node (e.g. RxVar). If the root value changes,
    * the effect code block will be executed.
    *
    * @param rx
    * @param effect
    * @tparam A
    * @tparam U
    * @return
    */
  private def run[A, U](rx: Rx[A])(effect: A => U): Cancelable = {
    rx match {
      case MapOp(in, f) =>
        run(in)(x => effect(f.asInstanceOf[Any => A](x)))
      case FlatMapOp(in, f) =>
        // This var is a placeholder to remember the preceding Cancelable operator, which will be updated later
        var c1 = Cancelable.empty
        val c2 = run(in) { x =>
          val rxb = f.asInstanceOf[Any => Rx[A]](x)
          // This code is necessary to properly cancel the effect if this operator is evaluated before
          c1.cancel
          c1 = run(rxb)(effect)
        }
        Cancelable { () => c1.cancel; c2.cancel }
      case FilterOp(in, cond) =>
        run(in) { x =>
          if (cond.asInstanceOf[A => Boolean](x)) {
            effect(x)
          }
        }
      case ZipOp(left, right) =>
        var allReady: Boolean = false
        var v1: Any           = null
        var v2: Any           = null
        val c1 = run(left) { x =>
          v1 = x
          if (allReady) {
            effect((v1, v2).asInstanceOf[A])
          }
        }
        val c2 = run(right) { x =>
          v2 = x
          if (allReady) {
            effect((v1, v2).asInstanceOf[A])
          }
        }
        allReady = true
        effect((v1, v2).asInstanceOf[A])
        Cancelable { () => c1.cancel; c2.cancel }
      case Zip3Op(r1, r2, r3) =>
        var allReady: Boolean = false
        var v1: Any           = null
        var v2: Any           = null
        var v3: Any           = null
        val c1 = run(r1) { x =>
          v1 = x
          if (allReady) {
            effect((v1, v2, v3).asInstanceOf[A])
          }
        }
        val c2 = run(r2) { x =>
          v2 = x
          if (allReady) {
            effect((v1, v2, v3).asInstanceOf[A])
          }
        }
        val c3 = run(r3) { x =>
          v3 = x
          if (allReady) {
            effect((v1, v2, v3).asInstanceOf[A])
          }
        }
        allReady = true
        effect((v1, v2, v3).asInstanceOf[A])
        Cancelable { () => c1.cancel; c2.cancel }
      case NamedOp(input, name) =>
        run(input)(effect)
      case SingleOp(v) =>
        effect(v)
        Cancelable.empty
      case v @ RxVar(currentValue) =>
        v.foreach(effect)
    }
  }

  private[rx] abstract class RxBase[A] extends Rx[A] {}

  abstract class UnaryRx[I, A] extends RxBase[A] {
    def input: Rx[I]
    override def parents: Seq[Rx[_]] = Seq(input)
  }

  case class SingleOp[A](v: A) extends RxBase[A] {
    override def parents: Seq[Rx[_]] = Seq.empty
  }
  case class MapOp[A, B](input: Rx[A], f: A => B)          extends UnaryRx[A, B]
  case class FlatMapOp[A, B](input: Rx[A], f: A => Rx[B])  extends UnaryRx[A, B]
  case class FilterOp[A](input: Rx[A], cond: A => Boolean) extends UnaryRx[A, A]
  case class ZipOp[A, B](left: Rx[A], right: Rx[B]) extends Rx[(A, B)] {
    override def parents: Seq[Rx[_]] = Seq(left, right)
  }
  case class Zip3Op[A, B, C](a: Rx[A], b: Rx[B], c: Rx[C]) extends Rx[(A, B, C)] {
    override def parents: Seq[Rx[_]] = Seq(a, b, c)
  }
  case class NamedOp[A](input: Rx[A], name: String) extends UnaryRx[A, A] {
    override def toString: String = s"${name}:${input}"
  }

  case class RxVar[A](private var currentValue: A) extends RxBase[A] {
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
        subscribers.map { s => s(newValue) }
      }
    }

    /**
      * Update the variable and force notification to subscribers
      * @param updater
      */
    def forceUpdate(updater: A => A): Unit = update(updater, force = true)
  }
}
