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
import wvlet.airframe.rx.Rx.{RecoverOp, RecoverWithOp}
import wvlet.log.LogSupport

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.{Failure, Success, Try}

trait Rx[+A] {
  def parents: Seq[Rx[_]]

  /**
    * Recover from a known error and emit a replacement value
    */
  def recover[U](f: PartialFunction[Throwable, U]): RxStream[U] = RecoverOp(this, f)

  /**
    * Recover from a known error and emit replacement values from a given Rx
    */
  def recoverWith[A](f: PartialFunction[Throwable, Rx[A]]): RxStream[A] = RecoverWithOp(this, f)

  def toRxStream: RxStream[A]

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

  /**
    * Materialize the stream as Seq[A]. This works only for the finite stream and for Scala JVM.
    */
  def toSeq: Seq[A] = {
    compat.toSeq(this)
  }
}

/**
  * The base reactive stream interface that can receive events from upstream operators and chain
  * next actions using Scala-collection like operators (e.g., map, filter, etc.)
  */
trait RxStream[+A] extends Rx[A] with LogSupport {
  import Rx._

  override def toRxStream: RxStream[A] = this
  def toOption[X, A1 >: A](implicit ev: A1 <:< Option[X]): RxOption[X] = RxOptionOp(
    this.asInstanceOf[RxStream[Option[X]]]
  )

  def withName(name: String): RxStream[A] = NamedOp(this, name)

  def map[B](f: A => B): RxStream[B]           = MapOp[A, B](this, f)
  def flatMap[B](f: A => Rx[B]): RxStream[B]   = FlatMapOp(this, f)
  def filter(f: A => Boolean): RxStream[A]     = FilterOp(this, f)
  def withFilter(f: A => Boolean): RxStream[A] = FilterOp(this, f)

  /**
    * Combine two Rx streams to form a sequence of pairs.
    * This will emit a new pair when both of the streams are updated.
    */
  def zip[B](other: Rx[B]): RxStream[(A, B)]                             = Rx.zip(this, other)
  def zip[B, C](b: Rx[B], c: Rx[C]): RxStream[(A, B, C)]                 = Rx.zip(this, b, c)
  def zip[B, C, D](b: Rx[B], c: Rx[C], d: Rx[D]): RxStream[(A, B, C, D)] = Rx.zip(this, b, c, d)

  /**
    * Emit a new output if one of Rx[A] or Rx[B] is changed.
    *
    * This method is useful when you need to monitor multiple Rx objects.
    *
    * Using joins will be more intuitive than nesting multiple Rx operators
    * like Rx[A].map { x => ... Rx[B].map { ...} }.
    */
  def join[B](other: Rx[B]): RxStream[(A, B)]                             = Rx.join(this, other)
  def join[B, C](b: Rx[B], c: Rx[C]): RxStream[(A, B, C)]                 = Rx.join(this, b, c)
  def join[B, C, D](b: Rx[B], c: Rx[C], d: Rx[D]): RxStream[(A, B, C, D)] = Rx.join(this, b, c, d)

  /**
    * Combine Rx stream and Future operators.
    *
    * This method is useful when you need to call RPC multiple times and
    * chain the next operation after receiving the response.
    * <code>
    * Rx.intervalMillis(1000)
    *   .andThen { i => callRpc(...) } // Returns Future
    *   .map { (rpcReturnValue) => ... } // Use the Future response
    * </code>
    */
  def andThen[B](f: A => Future[B])(implicit ex: ExecutionContext): RxStream[B] = {
    this.flatMap(a => Rx.future(f(a)))
  }

  def concat[A1 >: A](other: Rx[A1]): RxStream[A1] = Rx.concat(this, other)
  def lastOption: RxOption[A]                      = LastOp(this).toOption

  /**
    * Cache the last item, and emit the cached value if available.
    *
    * The cached value will be preserved to the operator itself even after cancelling the subscription.
    * Re-subscription of this operator will immediately return the cached
    * value to the downstream operator.
    *
    * This operator is useful if we need to involve time-consuming process, and want to reuse the last result:
    * <code>
    * val v = Rx.intervalMillis(1000).map(i => (heavy process)).cache
    *
    * v.map { x => ... }
    * </code>
    */
  def cache[A1 >: A]: RxStreamCache[A1] = CacheOp(this)

  /**
    * Take an event up to <i>n</i> elements. This may receive fewer events than n if the upstream operator
    * completes before generating <i>n</i> elements.
    */
  def take(n: Long): RxStream[A] = TakeOp(this, n)

  /**
    * Emit the first item of the source within each sampling period.
    * This is useful, for example, to prevent double-clicks of buttons.
    */
  def throttleFirst(timeWindow: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): RxStream[A] =
    ThrottleFirstOp[A](this, timeWindow, unit)

  /**
    * Emit the most recent item of the source within periodic time intervals.
    */
  def throttleLast(timeWindow: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): RxStream[A] =
    ThrottleLastOp[A](this, timeWindow, unit)

  /**
    * Emit the most recent item of the source within periodic time intervals.
    */
  def sample(timeWindow: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): RxStream[A] =
    ThrottleLastOp[A](this, timeWindow, unit)

  /**
    * Emit the given item first before returning the items from the source.
    */
  def startWith[A1 >: A](a: A1): RxStream[A1] = Rx.concat(Rx.single(a), this)

  /**
    * Emit the given items first before returning the items from the source.
    */
  def startWith[A1 >: A](lst: Seq[A1]): RxStream[A1] = Rx.concat(Rx.fromSeq(lst), this)
}

/**
  * RxStream[A] with a caching capability
  * @tparam A
  */
trait RxStreamCache[A] extends RxStream[A] {

  /**
    * Discard the cached value after the given duration.
    */
  def expireAfterWrite(time: Long, unit: TimeUnit): RxStreamCache[A]

  /**
    * Set a custom ticker. Use this only for testing purpose
    */
  def withTicker(ticker: Ticker): RxStreamCache[A]
}

object Rx extends LogSupport {
  def const[A](v: => A): RxStream[A]          = single(v)
  def single[A](v: => A): RxStream[A]         = SingleOp(LazyF0(v))
  def exception[A](e: Throwable): RxStream[A] = TryOp(Failure[A](e))

  /**
    * Create a sequence of values from Seq[A]
    */
  def fromSeq[A](lst: => Seq[A]): RxStream[A] = SeqOp(LazyF0(lst))

  /**
    * Create a sequence of values
    */
  def sequence[A](values: A*): RxStream[A] = fromSeq(values)
  def empty[A]: RxStream[A]                = fromSeq(Seq.empty)

  /**
    * @deprecated(description = "Use Rx.variable instead", since = "20.9.2")
    */
  def apply[A](v: A): RxVar[A]                        = variable(v)
  def variable[A](v: A): RxVar[A]                     = new RxVar(v)
  def optionVariable[A](v: Option[A]): RxOptionVar[A] = variable(v).toOption
  def option[A](v: => Option[A]): RxOption[A]         = RxOptionOp(single(v))
  val none: RxOption[Nothing]                         = RxOptionOp(single(None))

  def join[A, B](a: Rx[A], b: Rx[B]): RxStream[(A, B)]                                 = JoinOp(a, b)
  def join[A, B, C](a: Rx[A], b: Rx[B], c: Rx[C]): RxStream[(A, B, C)]                 = Join3Op(a, b, c)
  def join[A, B, C, D](a: Rx[A], b: Rx[B], c: Rx[C], d: Rx[D]): RxStream[(A, B, C, D)] = Join4Op(a, b, c, d)

  def zip[A, B](a: Rx[A], b: Rx[B]): RxStream[(A, B)]                                 = ZipOp(a, b)
  def zip[A, B, C](a: Rx[A], b: Rx[B], c: Rx[C]): RxStream[(A, B, C)]                 = Zip3Op(a, b, c)
  def zip[A, B, C, D](a: Rx[A], b: Rx[B], c: Rx[C], d: Rx[D]): RxStream[(A, B, C, D)] = Zip4Op(a, b, c, d)

  def concat[A, A1 >: A](a: Rx[A], b: Rx[A1]): RxStream[A1] = ConcatOp(a, b)

  /**
    * Periodically trigger an event and report the interval millis.
    * After running Rx with an interval, the cancel method must be called to stop the timer:
    * <code>
    *   val c = Rx.interval(...).run { x => ... }
    *   c.cancel
    * </code>
    */
  def interval(interval: Long, unit: TimeUnit): RxStream[Long] = IntervalOp(interval, unit)
  def intervalMillis(intervalMillis: Long): RxStream[Long]     = interval(intervalMillis, TimeUnit.MILLISECONDS)

  private def futureToRx[A](f: Future[A])(implicit ec: ExecutionContext): RxVar[Option[A]] = {
    val v = Rx.variable[Option[A]](None)
    f.foreach { x =>
      v := Some(x)
    }
    f.onComplete {
      case Success(_) =>
      case Failure(e) =>
        v.setException(e)
    }
    v
  }

  /**
    * Mapping a Scala Future into Rx. While the future response is unavailable, it emits Rx.none.
    * When the future is complete, Rx.some(A) will be returned.
    *
    * The difference from Rx.future is that this method can observe the waiting state of the Future response.
    *  For example, while this returns None, you can render an icon that represents loading state.
    */
  def fromFuture[A](f: Future[A])(implicit ec: ExecutionContext): RxOption[A] = {
    futureToRx(f)(ec).toOption
  }

  /**
    * Mapping a Scala Future into Rx that emits a value when the future is completed.
    */
  def future[A](f: Future[A])(implicit ec: ExecutionContext): RxStream[A] = {
    val v = futureToRx(f)(ec)
    v.filter(_.isDefined).map(_.get)
  }

  abstract class UnaryRx[I, A] extends RxStream[A] {
    def input: Rx[I]
    override def parents: Seq[Rx[_]] = Seq(input)
  }

  case class SingleOp[A](v: LazyF0[A]) extends RxStream[A] {
    override def parents: Seq[Rx[_]] = Seq.empty
  }
  case class SeqOp[A](lst: LazyF0[Seq[A]]) extends RxStream[A] {
    override def parents: Seq[Rx[_]] = Seq.empty
  }
  case class TryOp[A](v: Try[A]) extends RxStream[A] {
    override def parents: Seq[Rx[_]] = Seq.empty
  }

  case class MapOp[A, B](input: Rx[A], f: A => B)          extends UnaryRx[A, B]
  case class FlatMapOp[A, B](input: Rx[A], f: A => Rx[B])  extends UnaryRx[A, B]
  case class FilterOp[A](input: Rx[A], cond: A => Boolean) extends UnaryRx[A, A]
  case class ZipOp[A, B](a: Rx[A], b: Rx[B]) extends RxStream[(A, B)] {
    override def parents: Seq[Rx[_]] = Seq(a, b)
  }
  case class Zip3Op[A, B, C](a: Rx[A], b: Rx[B], c: Rx[C]) extends RxStream[(A, B, C)] {
    override def parents: Seq[Rx[_]] = Seq(a, b, c)
  }
  case class Zip4Op[A, B, C, D](a: Rx[A], b: Rx[B], c: Rx[C], d: Rx[D]) extends RxStream[(A, B, C, D)] {
    override def parents: Seq[Rx[_]] = Seq(a, b, c, d)
  }
  case class JoinOp[A, B](a: Rx[A], b: Rx[B]) extends RxStream[(A, B)] {
    override def parents: Seq[Rx[_]] = Seq(a, b)
  }
  case class Join3Op[A, B, C](a: Rx[A], b: Rx[B], c: Rx[C]) extends RxStream[(A, B, C)] {
    override def parents: Seq[Rx[_]] = Seq(a, b, c)
  }
  case class Join4Op[A, B, C, D](a: Rx[A], b: Rx[B], c: Rx[C], d: Rx[D]) extends RxStream[(A, B, C, D)] {
    override def parents: Seq[Rx[_]] = Seq(a, b, c, d)
  }

  case class ConcatOp[A](first: Rx[A], next: Rx[A]) extends RxStream[A] {
    override def parents: Seq[Rx[_]] = Seq(first, next)
  }
  case class LastOp[A](input: Rx[A]) extends RxStream[Option[A]] {
    override def parents: Seq[Rx[_]] = Seq(input)
  }
  case class NamedOp[A](input: Rx[A], name: String) extends UnaryRx[A, A] {
    override def toString: String = s"${name}:${input}"
  }
  case class RecoverOp[A, U](input: Rx[A], f: PartialFunction[Throwable, U])         extends UnaryRx[A, U]
  case class RecoverWithOp[A, U](input: Rx[A], f: PartialFunction[Throwable, Rx[U]]) extends UnaryRx[A, U]

  case class IntervalOp(interval: Long, unit: TimeUnit) extends RxStream[Long] {
    override def parents: Seq[Rx[_]] = Seq.empty
  }
  case class TakeOp[A](input: Rx[A], n: Long) extends RxStream[A] {
    override def parents: Seq[Rx[_]] = Seq(input)
  }
  case class ThrottleFirstOp[A](input: Rx[A], interval: Long, unit: TimeUnit) extends UnaryRx[A, A]
  case class ThrottleLastOp[A](input: Rx[A], interval: Long, unit: TimeUnit)  extends UnaryRx[A, A]

  case class CacheOp[A](
      input: Rx[A],
      var lastValue: Option[A] = None,
      var lastUpdatedNanos: Long = System.nanoTime(),
      expirationAfterWriteNanos: Option[Long] = None,
      ticker: Ticker = Ticker.systemTicker
  ) extends UnaryRx[A, A]
      with RxStreamCache[A] {
    override def expireAfterWrite(time: Long, unit: TimeUnit): RxStreamCache[A] = {
      this.copy(expirationAfterWriteNanos = Some(unit.toNanos(time)))
    }

    /**
      * Set a custom ticker. Use this only for testing purpose
      */
    override def withTicker(ticker: Ticker): RxStreamCache[A] = {
      this.copy(ticker = ticker)
    }
  }
}
