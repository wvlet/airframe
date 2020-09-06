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

import java.util.concurrent.TimeUnit

import wvlet.log.LogSupport

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.{Failure, Try}

/**
  */
trait Rx[+A] extends LogSupport {
  import Rx._

  def parents: Seq[Rx[_]]
  def withName(name: String): Rx[A] = NamedOp(this, name)

  def map[B](f: A => B): Rx[B]           = MapOp[A, B](this, f)
  def flatMap[B](f: A => Rx[B]): Rx[B]   = FlatMapOp(this, f)
  def filter(f: A => Boolean): Rx[A]     = FilterOp(this, f)
  def withFilter(f: A => Boolean): Rx[A] = FilterOp(this, f)

  /**
    * Combine two Rx streams to form a sequence of pairs.
    * This will emit a new pair when both of the streams are updated.
    */
  def zip[B](other: Rx[B]): Rx[(A, B)]             = ZipOp(this, other)
  def zip[B, C](b: Rx[B], c: Rx[C]): Rx[(A, B, C)] = Zip3Op(this, b, c)

  /**
    * Emit a new output if one of Rx[A] or Rx[B] is changed.
    *
    * This method is useful when you need to monitor multiple Rx objects.
    *
    * Using joins will be more intuitive than nesting multiple Rx operators
    * like Rx[A].map { x => ... Rx[B].map { ...} }.
    */
  def join[B](other: Rx[B]): Rx[(A, B)]                             = JoinOp(this, other)
  def join[B, C](b: Rx[B], c: Rx[C]): Rx[(A, B, C)]                 = Join3Op(this, b, c)
  def join[B, C, D](b: Rx[B], c: Rx[C], d: Rx[D]): Rx[(A, B, C, D)] = Join4Op(this, b, c, d)

  def concat[A1 >: A](other: Rx[A1]): Rx[A1] = ConcatOp(this, other)
  def lastOption: RxOption[A]                = LastOp(this).toOption

  /**
    * Take an event up to <i>n</i> elements. This may receive fewer events than n if the upstream operator
    * completes before generating <i>n</i> elements.
    */
  def take(n: Long): Rx[A] = TakeOp(this, n)

  def toOption[X, A1 >: A](implicit ev: A1 <:< Option[X]): RxOption[X] = RxOptionOp(this.asInstanceOf[Rx[Option[X]]])

  /**
    * Recover from a known error and emit a replacement value
    */
  def recover[U](f: PartialFunction[Throwable, U]): Rx[U] = RecoverOp(this, f)

  /**
    * Recover from a known error and emit replacement values from a given Rx
    */
  def recoverWith[A](f: PartialFunction[Throwable, Rx[A]]): Rx[A] = RecoverWithOp(this, f)

  /**
    * Materialize the streaming results as Seq
    * @return
    */
  def toSeq: Seq[A] = {
    val b = Seq.newBuilder[A]
    var c = Cancelable.empty
    c = RxRunner.run(this) {
      case OnNext(v) =>
        b += v.asInstanceOf[A]
      case OnError(e) =>
        c.cancel
        throw e
      case OnCompletion =>
        c.cancel
    }
    b.result()
  }

  /**
    * Subscribe any change in the upstream, and if a change is detected,
    *  the given subscriber code will be executed.
    *
    * @param subscriber
    * @tparam U
    * @return
    */
  def subscribe[U](subscriber: A => U): Cancelable = runContinuously(subscriber)

  /**
    * Evaluate this Rx[A] and apply the given effect function. Once OnError(e) or OnCompletion is observed,
    * it will stop the evaluation.
    *
    * @param effect
    * @tparam U
    * @return
    */
  def run[U](effect: A => U): Cancelable = {
    RxRunner.run(this) {
      case OnNext(v) =>
        effect(v.asInstanceOf[A])
      case OnError(e) =>
        throw e
      case OnCompletion =>
      // do nothing
    }
  }

  /**
    * Keep evaluating Rx[A] even if OnError(e) or OnCompletion is reported.
    * This is useful for keep processing streams.
    */
  def runContinuously[U](effect: A => U): Cancelable = {
    RxRunner.runContinuously(this) {
      case OnNext(v) =>
        effect(v.asInstanceOf[A])
      case OnError(e) =>
        throw e
      case OnCompletion =>
      // do nothing
    }
  }
}

object Rx extends LogSupport {
  def const[A](v: => A): Rx[A]          = single(v)
  def single[A](v: => A): Rx[A]         = SingleOp(LazyF0(v))
  def exception[A](e: Throwable): Rx[A] = TryOp(Failure[A](e))

  /**
    * Create a sequence of values from Seq[A]
    */
  def fromSeq[A](lst: => Seq[A]): Rx[A] = SeqOp(LazyF0(lst))

  /**
    * Create a sequence of values
    */
  def sequence[A](values: A*): Rx[A] = fromSeq(values)
  def empty[A]: Rx[A]                = fromSeq(Seq.empty)

  def apply[A](v: A): RxVar[A]                        = variable(v)
  def variable[A](v: A): RxVar[A]                     = new RxVar(v)
  def optionVariable[A](v: Option[A]): RxOptionVar[A] = variable(v).toOption
  def option[A](v: => Option[A]): RxOption[A]         = RxOptionOp(single(v))
  val none: RxOption[Nothing]                         = RxOptionOp(single(None))

  /**
    * Periodically trigger an event and report the interval millis.
    * After running Rx with an interval, the cancel method must be called to stop the timer:
    * <code>
    *   val c = Rx.interval(...).run { x => ... }
    *   c.cancel
    * </code>
    */
  def interval(interval: Long, unit: TimeUnit): Rx[Long] = IntervalOp(interval, unit)
  def intervalMillis(intervalMillis: Long): Rx[Long]     = interval(intervalMillis, TimeUnit.MILLISECONDS)

  /**
    * Mapping a Scala Future into Rx
    * @param f
    * @param ec
    * @tparam A
    * @return
    */
  def fromFuture[A](f: Future[A])(implicit ec: ExecutionContext): Rx[Option[A]] = {
    val v = Rx.variable[Option[A]](None)
    f.foreach { x =>
      v := Some(x)
    }
    v
  }

  abstract class UnaryRx[I, A] extends Rx[A] {
    def input: Rx[I]
    override def parents: Seq[Rx[_]] = Seq(input)
  }

  case class SingleOp[A](v: LazyF0[A]) extends Rx[A] {
    override def parents: Seq[Rx[_]] = Seq.empty
  }
  case class SeqOp[A](lst: LazyF0[Seq[A]]) extends Rx[A] {
    override def parents: Seq[Rx[_]] = Seq.empty
  }
  case class TryOp[A](v: Try[A]) extends Rx[A] {
    override def parents: Seq[Rx[_]] = Seq.empty
  }

  case class MapOp[A, B](input: Rx[A], f: A => B)          extends UnaryRx[A, B]
  case class FlatMapOp[A, B](input: Rx[A], f: A => Rx[B])  extends UnaryRx[A, B]
  case class FilterOp[A](input: Rx[A], cond: A => Boolean) extends UnaryRx[A, A]
  case class ZipOp[A, B](a: Rx[A], b: Rx[B]) extends Rx[(A, B)] {
    override def parents: Seq[Rx[_]] = Seq(a, b)
  }
  case class Zip3Op[A, B, C](a: Rx[A], b: Rx[B], c: Rx[C]) extends Rx[(A, B, C)] {
    override def parents: Seq[Rx[_]] = Seq(a, b, c)
  }
  case class JoinOp[A, B](a: Rx[A], b: Rx[B]) extends Rx[(A, B)] {
    override def parents: Seq[Rx[_]] = Seq(a, b)
  }
  case class Join3Op[A, B, C](a: Rx[A], b: Rx[B], c: Rx[C]) extends Rx[(A, B, C)] {
    override def parents: Seq[Rx[_]] = Seq(a, b, c)
  }
  case class Join4Op[A, B, C, D](a: Rx[A], b: Rx[B], c: Rx[C], d: Rx[D]) extends Rx[(A, B, C, D)] {
    override def parents: Seq[Rx[_]] = Seq(a, b, c, d)
  }

  case class ConcatOp[A](first: Rx[A], next: Rx[A]) extends Rx[A] {
    override def parents: Seq[Rx[_]] = Seq(first, next)
  }
  case class LastOp[A](input: Rx[A]) extends Rx[Option[A]] {
    override def parents: Seq[Rx[_]] = Seq(input)
  }
  case class NamedOp[A](input: Rx[A], name: String) extends UnaryRx[A, A] {
    override def toString: String = s"${name}:${input}"
  }
  case class RecoverOp[A, U](input: Rx[A], f: PartialFunction[Throwable, U])         extends UnaryRx[A, U]
  case class RecoverWithOp[A, U](input: Rx[A], f: PartialFunction[Throwable, Rx[U]]) extends UnaryRx[A, U]

  case class IntervalOp(interval: Long, unit: TimeUnit) extends Rx[Long] {
    override def parents: Seq[Rx[_]] = Seq.empty
  }
  case class TakeOp[A](input: Rx[A], n: Long) extends Rx[A] {
    override def parents: Seq[Rx[_]] = Seq(input)
  }
}
