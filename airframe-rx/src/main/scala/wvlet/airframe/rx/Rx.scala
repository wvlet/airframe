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
import scala.util.{Failure, Success, Try}

/**
  * A common interface for Rx and RxOption operators
  * @tparam A
  */
trait RxOps[+A] { self =>
  import Rx.*

  def parents: Seq[RxOps[_]]

  def toRx: Rx[A]

  /**
    * Recover from a known error and emit a replacement value
    */
  def recover[U](f: PartialFunction[Throwable, U]): Rx[U] = RecoverOp(this, f)

  /**
    * Recover from a known error and emit replacement values from a given Rx
    */
  def recoverWith[A](f: PartialFunction[Throwable, RxOps[A]]): Rx[A] = RecoverWithOp(this, f)

  /**
    * Applies `f` to the value for having a side effect, and return the original value.
    *
    * This method is useful for debugging Rx chains. For example:
    *
    * {{{
    *   rx.tapOn {
    *     case Success(v) => debug(s"received ${v}")
    *     case Failure(e) => error(s"request failed", e)
    *   }
    * }}}
    *
    * @param f
    *   partial function for the side effect
    * @return
    *   the original Rx event
    */
  def tapOn(f: PartialFunction[Try[A], Unit]): Rx[A] = TapOnOp(this, f)

  /**
    * Applies `f` to the value for having a side effect, and return the original value.
    *
    * The difference from [[tapOn]] is that this method will not receive an input failure.
    *
    * @param f
    *   side-effect function used when observing a value
    * @return
    *   the original Rx event
    */
  def tap(f: A => Unit): Rx[A] = tapOn({ case Success(v) => f(v) })

  /**
    * Applies `f` to the error if it happens, and return the original value.
    *
    * This method is useful for logging the error.
    *
    * @param f
    *   side-effect function used when observing an error
    * @return
    *   the original Rx event
    */
  def tapOnFailure(f: Throwable => Unit): Rx[A] = tapOn({ case Failure(e) => f(e) })

  /**
    * Evaluate this Rx[A] and apply the given effect function. Once OnError(e) or OnCompletion is observed, it will stop
    * the evaluation.
    *
    * @param effect
    * @tparam U
    * @return
    */
  def run[U](effect: A => U): Cancelable = {
    RxRunner.run(self) {
      case OnNext(v) =>
        effect(v.asInstanceOf[A])
      case OnError(e) =>
        throw e
      case OnCompletion =>
      // do nothing
    }
  }

  /**
    * Keep evaluating Rx[A] even if OnError(e) or OnCompletion is reported. This is useful for keep processing streams.
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

  def subscribe[U](subscriber: A => U): Cancelable = runContinuously(subscriber)
}

/**
  * The base reactive stream interface that can receive events from upstream operators and chain next actions using
  * Scala-collection like operators (e.g., map, filter, etc.)
  */
trait Rx[+A] extends RxOps[A] {

  /**
    * Materialize the stream as Seq[A]. This works only for the finite stream and for Scala JVM.
    */
  def toSeq: Seq[A] = {
    compat.toSeq(this)
  }

  import Rx.*

  override def toRx: Rx[A] = this
  def toOption[X, A1 >: A](implicit ev: A1 <:< Option[X]): RxOption[X] = RxOptionOp(
    this.asInstanceOf[Rx[Option[X]]]
  )

  def withName(name: String): Rx[A] = NamedOp(this, name)

  /**
    * Applies `f` to the input value and return the result.
    * @param f
    * @tparam B
    * @return
    */
  def map[B](f: A => B): Rx[B] = MapOp(this, f)

  /**
    * Applies `f` to the input value that produces another Rx stream. This method is an alias of flatMap(f)
    * @param f
    * @tparam B
    * @return
    */
  def mapToRx[B](f: A => RxOps[B]): Rx[B] = FlatMapOp(this, f)

  /**
    * Applies `f` to the input value that produces another Rx stream.
    * @param f
    * @tparam B
    * @return
    */
  def flatMap[B](f: A => RxOps[B]): Rx[B] = FlatMapOp(this, f)

  /**
    * Applies the given filter and emit the value only when the filter condition matches
    * @param f
    * @return
    */
  def filter(f: A => Boolean): Rx[A] = FilterOp(this, f)

  /**
    * An alias of filter
    * @param f
    * @return
    */
  def withFilter(f: A => Boolean): Rx[A] = FilterOp(this, f)

  /**
    * An alias of filter
    * @param conf
    * @return
    */
  def when(cond: A => Boolean): Rx[A] = FilterOp(this, cond)

  /**
    * Combine two Rx streams to form a sequence of pairs. This will emit a new pair when both of the streams are
    * updated.
    */
  def zip[B](other: RxOps[B]): Rx[(A, B)] = Rx.zip(this, other)

  /**
    * Combine three Rx streams to form a sequence of triples. This will emit a new triple when all of the streams are
    * updated.
    * @param b
    * @param c
    * @tparam B
    * @tparam C
    * @return
    */
  def zip[B, C](b: RxOps[B], c: RxOps[C]): Rx[(A, B, C)] = Rx.zip(this, b, c)

  /**
    * Combine four Rx streams to form a sequence of quadruples. This will emit a new quadruple when all of the streams
    * are updated.
    * @param b
    * @param c
    * @param d
    * @tparam B
    * @tparam C
    * @tparam D
    * @return
    */
  def zip[B, C, D](b: RxOps[B], c: RxOps[C], d: RxOps[D]): Rx[(A, B, C, D)] = Rx.zip(this, b, c, d)

  /**
    * Combine five Rx streams to form a sequence of quintuples. This will emit a new quintuple when all of the streams
    * are updated.
    * @param b
    * @param c
    * @param d
    * @param e
    * @tparam B
    * @tparam C
    * @tparam D
    * @tparam E
    * @return
    */
  def zip[B, C, D, E](b: RxOps[B], c: RxOps[C], d: RxOps[D], e: RxOps[E]): Rx[(A, B, C, D, E)] =
    Rx.zip(this, b, c, d, e)

  /**
    * Emit a new output if one of Rx[A] or Rx[B] is changed.
    *
    * This method is useful when you need to monitor multiple Rx objects.
    *
    * Using joins will be more intuitive than nesting multiple Rx operators like Rx[A].map { x => ... Rx[B].map { ...}
    * }.
    */
  def join[B](other: RxOps[B]): Rx[(A, B)] = Rx.join(this, other)

  /**
    * Emit a new output if one of Rx[A], Rx[B], or Rx[C] is changed.
    * @param b
    * @param c
    * @tparam B
    * @tparam C
    * @return
    */
  def join[B, C](b: RxOps[B], c: RxOps[C]): Rx[(A, B, C)] = Rx.join(this, b, c)

  /**
    * Emit a new output if one of Rx[A], Rx[B], Rx[C], or Rx[D] is changed.
    * @param b
    * @param c
    * @param d
    * @tparam B
    * @tparam C
    * @tparam D
    * @return
    */
  def join[B, C, D](b: RxOps[B], c: RxOps[C], d: RxOps[D]): Rx[(A, B, C, D)] = Rx.join(this, b, c, d)

  /**
    * Emit a new output if one of Rx[A], Rx[B], Rx[C], Rx[D], or Rx[E] is changed.
    * @param b
    * @param c
    * @param d
    * @param e
    * @tparam B
    * @tparam C
    * @tparam D
    * @tparam E
    * @return
    */
  def join[B, C, D, E](b: RxOps[B], c: RxOps[C], d: RxOps[D], e: RxOps[E]): Rx[(A, B, C, D, E)] =
    Rx.join(this, b, c, d, e)

  /**
    * Combine Rx stream and Future operators.
    *
    * This method is useful when you need to call RPC multiple times and chain the next operation after receiving the
    * response.
    * {{{
    * Rx.intervalMillis(1000)
    *   .andThen { i => callRpc(...) } // Returns Future
    *   .map { (rpcReturnValue) => ... } // Use the Future response
    * }}}
    */
  def andThen[B](f: A => Future[B])(implicit ex: ExecutionContext): Rx[B] = {
    this.flatMap(a => Rx.future(f(a)))
  }

  /**
    * Transform the input value by wrapping it with Try regardless of success or failure. This is useful when you need
    * to handle both success and failure cases in the same way.
    */
  def transformRx[B](f: Try[A] => RxOps[B]): Rx[B] = {
    TransformRxOp(this, f)
  }

  def transform[B](f: Try[A] => B): Rx[B] = {
    TransformOp(this, f)
  }

  /**
    * Transform the input value by wrapping it with Try regardless of success or failure. This is useful when you need
    * to add a post-processing step after handling success and failure cases.
    */
  def transformTry[B](f: Try[A] => Try[B]): Rx[B] = {
    TransformTryOp(this, f)
  }

  def concat[A1 >: A](other: Rx[A1]): Rx[A1] = Rx.concat(this, other)
  def lastOption: RxOption[A]                = LastOp(this).toOption

  /**
    * Cache the last item, and emit the cached value if available.
    *
    * The cached value will be preserved to the operator itself even after cancelling the subscription. Re-subscription
    * of this operator will immediately return the cached value to the downstream operator.
    *
    * This operator is useful if we need to involve time-consuming process, and want to reuse the last result: <code>
    * val v = Rx.intervalMillis(1000).map(i => (heavy process)).cache
    *
    * v.map { x => ... } </code>
    */
  def cache[A1 >: A]: RxCache[A1] = CacheOp(this)

  /**
    * Take an event up to <i>n</i> elements. This may receive fewer events than n if the upstream operator completes
    * before generating <i>n</i> elements.
    */
  def take(n: Long): Rx[A] = TakeOp(this, n)

  /**
    * Emit the first item of the source within each sampling period. For example, this is useful to prevent
    * double-clicks of buttons.
    */
  def throttleFirst(timeWindow: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Rx[A] =
    ThrottleFirstOp[A](this, timeWindow, unit)

  /**
    * Emit the most recent item of the source within periodic time intervals.
    */
  def throttleLast(timeWindow: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Rx[A] =
    ThrottleLastOp[A](this, timeWindow, unit)

  /**
    * Emit the most recent item of the source within periodic time intervals.
    */
  def sample(timeWindow: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Rx[A] =
    ThrottleLastOp[A](this, timeWindow, unit)

  /**
    * Emit the given item first before returning the items from the source.
    */
  def startWith[A1 >: A](a: A1): Rx[A1] = Rx.concat(Rx.single(a), this)

  /**
    * Emit the given items first before returning the items from the source.
    */
  def startWith[A1 >: A](lst: Seq[A1]): Rx[A1] = Rx.concat(Rx.fromSeq(lst), this)
}

/**
  * Rx[A] with a caching capability
  * @tparam A
  */
trait RxCache[A] extends Rx[A] {

  /**
    * Get the current cached value if exists
    */
  def getCurrent: Option[A]

  /**
    * Discard the cached value after the given duration.
    */
  def expireAfterWrite(time: Long, unit: TimeUnit): RxCache[A]

  /**
    * Set a custom ticker. Use this only for testing purpose
    */
  def withTicker(ticker: Ticker): RxCache[A]
}

object Rx extends LogSupport {

  /**
    * Provide a constant value by immediately evaluating the given input
    */
  def const[A](v: => A): Rx[A] = {
    // wrap the value with Try to propagate exception through Rx
    fromTry(Try(v))
  }

  /**
    * Create a lazily evaluated single value
    */
  def single[A](v: => A): Rx[A]         = SingleOp(LazyF0.apply(v))
  def exception[A](e: Throwable): Rx[A] = fromTry(Failure[A](e))

  /**
    * Create a sequence of values from Seq[A]
    */
  def fromSeq[A](lst: => Seq[A]): Rx[A] = SeqOp(LazyF0(lst))

  def fromTry[A](t: Try[A]): Rx[A] = TryOp(LazyF0(t))

  /**
    * Create a sequence of values
    */
  def sequence[A](values: A*): Rx[A] = fromSeq(values)
  def empty[A]: Rx[A]                = fromSeq(Seq.empty)

  @deprecated(message = "Use Rx.variable instead", since = "20.9.2")
  def apply[A](v: A): RxVar[A]                        = variable(v)
  def variable[A](v: A): RxVar[A]                     = new RxVar(v)
  def optionVariable[A](v: Option[A]): RxOptionVar[A] = variable(v).toOption
  def option[A](v: => Option[A]): RxOption[A]         = RxOptionOp(single(v))
  def some[A](v: => A): RxOption[A]                   = option(Some(v))
  val none: RxOption[Nothing]                         = option(None)

  def join[A, B](a: RxOps[A], b: RxOps[B]): Rx[(A, B)]                                       = JoinOp(a, b)
  def join[A, B, C](a: RxOps[A], b: RxOps[B], c: RxOps[C]): Rx[(A, B, C)]                    = Join3Op(a, b, c)
  def join[A, B, C, D](a: RxOps[A], b: RxOps[B], c: RxOps[C], d: RxOps[D]): Rx[(A, B, C, D)] = Join4Op(a, b, c, d)
  def join[A, B, C, D, E](a: RxOps[A], b: RxOps[B], c: RxOps[C], d: RxOps[D], e: RxOps[E]): Rx[(A, B, C, D, E)] =
    Join5Op(a, b, c, d, e)

  def zip[A, B](a: RxOps[A], b: RxOps[B]): Rx[(A, B)]                                       = ZipOp(a, b)
  def zip[A, B, C](a: RxOps[A], b: RxOps[B], c: RxOps[C]): Rx[(A, B, C)]                    = Zip3Op(a, b, c)
  def zip[A, B, C, D](a: RxOps[A], b: RxOps[B], c: RxOps[C], d: RxOps[D]): Rx[(A, B, C, D)] = Zip4Op(a, b, c, d)
  def zip[A, B, C, D, E](a: RxOps[A], b: RxOps[B], c: RxOps[C], d: RxOps[D], e: RxOps[E]): Rx[(A, B, C, D, E)] =
    Zip5Op(a, b, c, d, e)

  def concat[A, A1 >: A](a: RxOps[A], b: RxOps[A1]): Rx[A1] = ConcatOp(a, b)

  /**
    * Periodically trigger an event and report the interval millis. After running Rx with an interval, the cancel method
    * must be called to stop the timer: <code> val c = Rx.interval(...).run { x => ... } c.cancel </code>
    */
  def interval(interval: Long, unit: TimeUnit): Rx[Long] = IntervalOp(interval, unit)
  def intervalMillis(intervalMillis: Long): Rx[Long]     = interval(intervalMillis, TimeUnit.MILLISECONDS)

  /**
    * Emits 0 once after the give delay period.
    */
  def timer(interval: Long, unit: TimeUnit): Rx[Long] = TimerOp(interval, unit)

  /**
    * Emits 0 once after the give delay period.
    */
  def delay(interval: Long, unit: TimeUnit): Rx[Long] = timer(interval, unit)

  private def futureToRx[A](f: Future[A])(implicit ec: ExecutionContext): RxVar[Option[A]] = {
    val v = Rx.variable[Option[A]](None)
    f.foreach { x =>
      v := Some(x)
      // Send OnCompletion event to the variable as the value will have no more update
      v.stop()
    }
    f.onComplete {
      case Success(_) =>
      case Failure(e) =>
        v.setException(e)
    }
    v
  }

  /**
    * Mapping a Scala Future into Rx. While the future response is unavailable, it emits Rx.none. When the future is
    * complete, Rx.some(A) will be returned.
    *
    * The difference from Rx.future is that this method can observe the waiting state of the Future response. For
    * example, while this returns None, you can render an icon that represents loading state.
    */
  def fromFuture[A](f: Future[A])(implicit ec: ExecutionContext): RxOption[A] = {
    futureToRx(f)(ec).toOption
  }

  /**
    * Mapping a Scala Future into Rx that emits a value when the future is completed.
    */
  def future[A](f: Future[A])(implicit ec: ExecutionContext): Rx[A] = {
    val v = futureToRx(f)(ec)
    v.filter(_.isDefined).map(_.get)
  }

  abstract class UnaryRx[I, A] extends Rx[A] {
    def input: RxOps[I]
    override def parents: Seq[RxOps[_]] = Seq(input)
  }

  case class SingleOp[A](v: LazyF0[A]) extends Rx[A] {
    override def parents: Seq[RxOps[_]] = Seq.empty
  }
  case class SeqOp[A](lst: LazyF0[Seq[A]]) extends Rx[A] {
    override def parents: Seq[RxOps[_]] = Seq.empty
  }
  case class TryOp[A](v: LazyF0[Try[A]]) extends Rx[A] {
    override def parents: Seq[RxOps[_]] = Seq.empty
  }
  case class TransformRxOp[A, B](input: Rx[A], f: Try[A] => RxOps[B]) extends Rx[B] {
    override def parents: Seq[RxOps[_]] = Seq(input)
  }
  case class TransformOp[A, B](input: Rx[A], f: Try[A] => B) extends Rx[B] {
    override def parents: Seq[RxOps[_]] = Seq(input)
  }

  case class TransformTryOp[A, B](input: Rx[A], f: Try[A] => Try[B]) extends Rx[B] {
    override def parents: Seq[RxOps[_]] = Seq(input)
  }

  case class MapOp[A, B](input: Rx[A], f: A => B)            extends UnaryRx[A, B]
  case class FlatMapOp[A, B](input: Rx[A], f: A => RxOps[B]) extends UnaryRx[A, B]
  case class FilterOp[A](input: Rx[A], cond: A => Boolean)   extends UnaryRx[A, A]
  case class ZipOp[A, B](a: RxOps[A], b: RxOps[B]) extends Rx[(A, B)] {
    override def parents: Seq[RxOps[_]] = Seq(a, b)
  }
  case class Zip3Op[A, B, C](a: RxOps[A], b: RxOps[B], c: RxOps[C]) extends Rx[(A, B, C)] {
    override def parents: Seq[RxOps[_]] = Seq(a, b, c)
  }
  case class Zip4Op[A, B, C, D](a: RxOps[A], b: RxOps[B], c: RxOps[C], d: RxOps[D]) extends Rx[(A, B, C, D)] {
    override def parents: Seq[RxOps[_]] = Seq(a, b, c, d)
  }
  case class Zip5Op[A, B, C, D, E](a: RxOps[A], b: RxOps[B], c: RxOps[C], d: RxOps[D], e: RxOps[E])
      extends Rx[(A, B, C, D, E)] {
    override def parents: Seq[RxOps[_]] = Seq(a, b, c, d, e)
  }

  case class JoinOp[A, B](a: RxOps[A], b: RxOps[B]) extends Rx[(A, B)] {
    override def parents: Seq[RxOps[_]] = Seq(a, b)
  }
  case class Join3Op[A, B, C](a: RxOps[A], b: RxOps[B], c: RxOps[C]) extends Rx[(A, B, C)] {
    override def parents: Seq[RxOps[_]] = Seq(a, b, c)
  }
  case class Join4Op[A, B, C, D](a: RxOps[A], b: RxOps[B], c: RxOps[C], d: RxOps[D]) extends Rx[(A, B, C, D)] {
    override def parents: Seq[RxOps[_]] = Seq(a, b, c, d)
  }
  case class Join5Op[A, B, C, D, E](a: RxOps[A], b: RxOps[B], c: RxOps[C], d: RxOps[D], e: RxOps[E])
      extends Rx[(A, B, C, D, E)] {
    override def parents: Seq[RxOps[_]] = Seq(a, b, c, d, e)
  }

  case class ConcatOp[A](first: RxOps[A], next: RxOps[A]) extends Rx[A] {
    override def parents: Seq[RxOps[_]] = Seq(first, next)
  }
  case class LastOp[A](input: RxOps[A]) extends Rx[Option[A]] {
    override def parents: Seq[RxOps[_]] = Seq(input)
  }
  case class NamedOp[A](input: RxOps[A], name: String) extends UnaryRx[A, A] {
    override def toString: String = s"${name}:${input}"
  }
  case class RecoverOp[A, U](input: RxOps[A], f: PartialFunction[Throwable, U])            extends UnaryRx[A, U]
  case class RecoverWithOp[A, U](input: RxOps[A], f: PartialFunction[Throwable, RxOps[U]]) extends UnaryRx[A, U]

  case class TapOnOp[A](input: RxOps[A], f: PartialFunction[Try[A], Unit]) extends UnaryRx[A, A]

  case class IntervalOp(interval: Long, unit: TimeUnit) extends Rx[Long] {
    override def parents: Seq[RxOps[_]] = Seq.empty
  }
  case class TimerOp(interval: Long, unit: TimeUnit) extends Rx[Long] {
    override def parents: Seq[RxOps[_]] = Seq.empty
  }

  case class TakeOp[A](input: RxOps[A], n: Long) extends Rx[A] {
    override def parents: Seq[RxOps[_]] = Seq(input)
  }
  case class ThrottleFirstOp[A](input: RxOps[A], interval: Long, unit: TimeUnit) extends UnaryRx[A, A]
  case class ThrottleLastOp[A](input: RxOps[A], interval: Long, unit: TimeUnit)  extends UnaryRx[A, A]

  case class CacheOp[A](
      input: RxOps[A],
      var lastValue: Option[A] = None,
      var lastUpdatedNanos: Long = System.nanoTime(),
      expirationAfterWriteNanos: Option[Long] = None,
      ticker: Ticker = Ticker.systemTicker
  ) extends UnaryRx[A, A]
      with RxCache[A] {
    override def getCurrent: Option[A] = lastValue
    override def expireAfterWrite(time: Long, unit: TimeUnit): RxCache[A] = {
      this.copy(expirationAfterWriteNanos = Some(unit.toNanos(time)))
    }

    /**
      * Set a custom ticker. Use this only for testing purpose
      */
    override def withTicker(ticker: Ticker): RxCache[A] = {
      this.copy(ticker = ticker)
    }
  }
}
