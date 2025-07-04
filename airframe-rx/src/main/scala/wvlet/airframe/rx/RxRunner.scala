package wvlet.airframe.rx

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import wvlet.log.LogSupport

import scala.annotation.tailrec
import scala.collection.immutable.Queue
import scala.util.{Failure, Success, Try}

/**
  * States for propagating the result of the downstream operators.
  *
  * TODO: Add a state for telling how many elements can be received in downstream operators for implementing
  * back-pressure
  */
sealed trait RxResult {
  def toContinue: Boolean
  def &&(other: RxResult): RxResult = {
    if (this.toContinue && other.toContinue) {
      RxResult.Continue
    } else {
      RxResult.Stop
    }
  }
}

object RxResult {
  object Continue extends RxResult {
    override def toContinue: Boolean = true
  }
  object Stop extends RxResult {
    override def toContinue: Boolean = false
  }
}

object RxRunner extends LogSupport {

  private val defaultRunner = new RxRunner(continuous = false)
  // Used for continuous RxVar evaluation (e.g., RxVar -> DOM rendering).
  // This runner will not report OnCompleiton event
  private val continuousRunner = new RxRunner(continuous = true)

  def run[A, U](rx: RxOps[A])(effect: RxEvent => U): Cancelable = {
    defaultRunner.run(rx) { ev =>
      ev match {
        case v @ OnNext(_) =>
          effect(v)
          RxResult.Continue
        case other =>
          effect(other)
          RxResult.Stop
      }
    }
  }

  /**
    * Run until the first event is observed
    */
  def runOnce[A, U](rx: RxOps[A])(effect: RxEvent => U): Cancelable = {
    defaultRunner.run(rx) { ev =>
      ev match {
        case v @ OnNext(_) =>
          effect(v)
          RxResult.Stop
        case other =>
          effect(other)
          RxResult.Stop
      }
    }
  }

  def runContinuously[A, U](rx: RxOps[A])(effect: RxEvent => U): Cancelable = {
    continuousRunner.run(rx) { ev =>
      ev match {
        case v @ OnNext(_) =>
          effect(v)
          RxResult.Continue
        case other =>
          effect(other)
          RxResult.Stop
      }
    }
  }
}

class RxRunner(
    // If this value is true, evaluating Rx keeps reporting events after OnError or OnCompletion is observed
    continuous: Boolean
) extends LogSupport { runner =>

  import Rx.*

  /**
    * Build an executable chain of Rx operators. The resulting chain will be registered as a subscriber to the root node
    * (see RxVar.foreach). If the root value changes, the effect code block will be executed.
    *
    * @param rx
    * @param effect
    *   a function to process the generated RxEvent. This function must return [[RxResult.Continue]] when the downstream
    *   operator can receive further events (OnNext). If the leaf sink operator issued OnError or OnCompletion event,
    *   this must return [[RxResult.Stop]].
    * @tparam A
    */
  def run[A](rx: RxOps[A])(effect: RxEvent => RxResult): Cancelable = {
    rx match {
      case MapOp(in, f) =>
        run(in) {
          case OnNext(v) =>
            Try(f.asInstanceOf[Any => A](v)) match {
              case Success(x) =>
                effect(OnNext(x))
              case Failure(e) =>
                effect(OnError(e))
            }
          case other =>
            effect(other)
        }
      case fm @ FlatMapOp(in, f) =>
        // This var is a placeholder to remember the preceding Cancelable operator, which will be updated later
        var c1 = Cancelable.empty
        val c2 = run(fm.input) {
          case OnNext(x) =>
            var toContinue: RxResult = RxResult.Continue
            Try(fm.f.asInstanceOf[Function[Any, RxOps[_]]](x)) match {
              case Success(rxb) =>
                // This code is necessary to properly cancel the effect if this operator is evaluated before
                c1.cancel
                c1 = run(rxb) {
                  case n @ OnNext(x) =>
                    toContinue = effect(n)
                    toContinue
                  case OnCompletion =>
                    // skip the end of the nested flatMap body stream
                    RxResult.Continue
                  case ev @ OnError(e) =>
                    toContinue = effect(ev)
                    toContinue
                }
                toContinue
              case Failure(e) =>
                effect(OnError(e))
            }
          case other =>
            effect(other)
        }
        Cancelable { () =>
          c1.cancel; c2.cancel
        }
      case FilterOp(in, cond) =>
        run(in) { ev =>
          ev match {
            case OnNext(x) =>
              Try(cond.asInstanceOf[A => Boolean](x.asInstanceOf[A])) match {
                case Success(true) =>
                  effect(OnNext(x))
                case Success(false) =>
                  // Notify the completion of the stream, but continue subscription to the upstream
                  effect(OnCompletion)
                  RxResult.Continue
                case Failure(e) =>
                  effect(OnError(e))
              }
            case other =>
              effect(other)
          }
        }
      case TransformOp(in, f) =>
        val tryFunc = f.asInstanceOf[Try[_] => _]
        run(in) { ev =>
          ev match {
            case OnNext(x) =>
              Try(tryFunc(Success(x))) match {
                case Success(x) =>
                  effect(OnNext(x))
                case Failure(e) =>
                  effect(OnError(e))
              }
            case OnError(e) =>
              Try(tryFunc(Failure(e))) match {
                case Success(x) =>
                  effect(OnNext(x))
                case Failure(e) =>
                  effect(OnError(e))
              }
            case other =>
              effect(other)
          }
        }
      case TransformTryOp(in, f) =>
        val tryFunc = f.asInstanceOf[Try[_] => Try[_]]
        run(in) { ev =>
          ev match {
            case OnNext(x) =>
              tryFunc(Success(x)) match {
                case Success(x) =>
                  effect(OnNext(x))
                case Failure(e) =>
                  effect(OnError(e))
              }
            case OnError(e) =>
              tryFunc(Failure(e)) match {
                case Success(x) =>
                  effect(OnNext(x))
                case Failure(e) =>
                  effect(OnError(e))
              }
            case other =>
              effect(other)
          }
        }
      case TransformRxOp(in, f) =>
        val tryFunc = f.asInstanceOf[Try[_] => RxOps[_]]
        // A place holder for properly cancel the subscription against the result of Try[_] => Rx[_]
        var c1: Cancelable = Cancelable.empty

        def evalRx(rxb: RxOps[_]): RxResult = {
          c1.cancel
          c1 = run(rxb) {
            case OnNext(x) =>
              effect(OnNext(x))
            case OnCompletion =>
              RxResult.Continue
            case OnError(e) =>
              effect(OnError(e))
          }
          RxResult.Continue
        }

        // Call f: Try[_] => Rx[_] using the input
        val c2: Cancelable = run(in) {
          case OnNext(x) =>
            Try(tryFunc(Success(x))) match {
              case Success(rxb) =>
                evalRx(rxb)
              case Failure(e) =>
                effect(OnError(e))
            }
          case OnError(e) =>
            Try(tryFunc(Failure(e))) match {
              case Success(rxb) =>
                evalRx(rxb)
              case Failure(e) =>
                effect(OnError(e))
            }
          case other =>
            effect(other)
        }
        Cancelable { () =>
          c1.cancel; c2.cancel
        }
      case ConcatOp(first, next) =>
        var c1 = Cancelable.empty
        val c2 = run(first) {
          case OnCompletion =>
            var toContinue: RxResult = RxResult.Continue
            // Properly cancel the effect if this operator is evaluated before
            c1.cancel
            c1 = run(next) { ev =>
              toContinue = effect(ev)
              toContinue
            }
            toContinue
          case other =>
            effect(other)
        }
        Cancelable { () =>
          c1.cancel; c2.cancel
        }
      case LastOp(in) =>
        var last: Option[A] = None
        run(in) {
          case OnNext(v) =>
            last = Some(v.asInstanceOf[A])
            RxResult.Continue
          case err @ OnError(e) =>
            effect(err)
          case OnCompletion =>
            Try(effect(OnNext(last))) match {
              case Success(v) => effect(OnCompletion)
              case Failure(e) => effect(OnError(e))
            }
        }
      case cache @ CacheOp(in, lastValue, lastUpdatedNanos, _, ticker) =>
        lastValue match {
          case Some(v) =>
            val currentNanos         = ticker.currentNanos
            val nanosSinceLastUpdate = currentNanos - lastUpdatedNanos
            val isExpired =
              cache.expirationAfterWriteNanos
                .map(expireNanos => expireNanos <= nanosSinceLastUpdate)
                .getOrElse(false)
            if (!isExpired) {
              effect(OnNext(v))
            }
          case None =>
        }
        run(in) {
          case OnNext(v) =>
            cache.asInstanceOf[CacheOp[A]].lastValue = Some(v.asInstanceOf[A])
            cache.lastUpdatedNanos = ticker.currentNanos
            effect(OnNext(v))
          case other =>
            effect(other)
        }
      case TakeOp(in, n) =>
        var count = 0
        run(in) {
          case OnNext(v) =>
            if (count < n) {
              count += 1
              effect(OnNext(v.asInstanceOf[A]))
            } else {
              effect(OnCompletion)
              RxResult.Stop
            }
          case err @ OnError(e) =>
            effect(err)
          case OnCompletion =>
            effect(OnCompletion)
        }
      case IntervalOp(interval, unit) =>
        val intervalMillis =
          TimeUnit.MILLISECONDS.convert(interval, unit).max(1)
        val timer: Timer = compat.newTimer
        timer.schedule(intervalMillis) { interval =>
          val canContinue = effect(OnNext(interval))
          if (!canContinue.toContinue) {
            timer.cancel
          }
        }
        Cancelable { () =>
          timer.cancel
        }
      case TimerOp(interval, unit) =>
        val delayMillis = TimeUnit.MILLISECONDS.convert(interval, unit).max(1)
        compat.scheduleOnce(delayMillis) {
          Try(effect(OnNext(0L))) match {
            case Success(c) =>
              effect(OnCompletion)
            case Failure(e) =>
              effect(OnError(e))
          }
        }
      case ThrottleFirstOp(in, interval, unit) =>
        var lastUpdateTimeNanos = -interval
        run(in) {
          case next @ OnNext(v) =>
            val currentTimeNanos = System.nanoTime()
            val elapsed          = unit.convert(currentTimeNanos - lastUpdateTimeNanos, TimeUnit.NANOSECONDS)
            if (elapsed >= interval) {
              lastUpdateTimeNanos = currentTimeNanos
              effect(next)
            } else {
              // Do not emit the value, but continue the subscription
              RxResult.Continue
            }
          case other =>
            effect(other)
        }
      case ThrottleLastOp(in, interval, unit) =>
        val intervalMillis =
          TimeUnit.MILLISECONDS.convert(interval, unit).max(1)
        var lastItem: Option[A]     = None
        var lastReported: Option[A] = None
        val timer: Timer            = compat.newTimer
        var canContinue: RxResult   = RxResult.Continue
        timer.schedule(intervalMillis) { interval =>
          lastItem match {
            case Some(x) =>
              lastItem = Some(x)
              if (lastReported != lastItem) {
                lastReported = lastItem
                canContinue = effect(OnNext(x))
                if (!canContinue.toContinue) {
                  timer.cancel
                }
              }
            case None =>
            // Do nothing
          }
        }
        val c = run(in) {
          case OnNext(v) =>
            lastItem = Some(v.asInstanceOf[A])
            canContinue
          case other =>
            canContinue && effect(other)
        }
        Cancelable { () =>
          timer.cancel
          c.cancel
        }
      case DelayOp(in, interval, unit) =>
        val delayMillis = TimeUnit.MILLISECONDS.convert(interval, unit).max(1)
        run(in) {
          case OnNext(v) =>
            // Schedule the delayed emission asynchronously
            compat.scheduleOnce(delayMillis) {
              effect(OnNext(v))
            }
            RxResult.Continue
          case other =>
            effect(other)
        }
      case z @ ZipOp(r1, r2) =>
        zip(z)(effect)
      case z @ Zip3Op(r1, r2, r3) =>
        zip(z)(effect)
      case z @ Zip4Op(r1, r2, r3, r4) =>
        zip(z)(effect)
      case z @ Zip5Op(r1, r2, r3, r4, r5) =>
        zip(z)(effect)
      case z @ Zip6Op(r1, r2, r3, r4, r5, r6) =>
        zip(z)(effect)
      case z @ Zip7Op(r1, r2, r3, r4, r5, r6, r7) =>
        zip(z)(effect)
      case z @ Zip8Op(r1, r2, r3, r4, r5, r6, r7, r8) =>
        zip(z)(effect)
      case z @ Zip9Op(r1, r2, r3, r4, r5, r6, r7, r8, r9) =>
        zip(z)(effect)
      case z @ Zip10Op(r1, r2, r3, r4, r5, r6, r7, r8, r9, r10) =>
        zip(z)(effect)
      case j @ JoinOp(r1, r2) =>
        join(j)(effect)
      case j @ Join3Op(r1, r2, r3) =>
        join(j)(effect)
      case j @ Join4Op(r1, r2, r3, r4) =>
        join(j)(effect)
      case j @ Join5Op(r1, r2, r3, r4, r5) =>
        join(j)(effect)
      case j @ Join6Op(r1, r2, r3, r4, r5, r6) =>
        join(j)(effect)
      case j @ Join7Op(r1, r2, r3, r4, r5, r6, r7) =>
        join(j)(effect)
      case j @ Join8Op(r1, r2, r3, r4, r5, r6, r7, r8) =>
        join(j)(effect)
      case j @ Join9Op(r1, r2, r3, r4, r5, r6, r7, r8, r9) =>
        join(j)(effect)
      case j @ Join10Op(r1, r2, r3, r4, r5, r6, r7, r8, r9, r10) =>
        join(j)(effect)
      case RxOptionOp(in) =>
        run(in) {
          case e @ OnNext(v) =>
            effect(e)
          case other =>
            effect(other)
        }
      case RxOptionCacheOp(input) =>
        run(input)(effect)
      case NamedOp(input, name) =>
        run(input)(effect)
      case TryOp(e) =>
        e.eval match {
          case Success(x) =>
            effect(OnNext(x))
          case Failure(e) =>
            effect(OnError(e))
        }
        Cancelable.empty
      case o: RxOptionVar[_] =>
        o.asInstanceOf[RxOptionVar[A]].foreachEvent { ev =>
          effect(ev)
        }
      case v: RxVar[_] =>
        v.asInstanceOf[RxVar[A]].foreachEvent { ev =>
          effect(ev)
        }
      case RecoverOp(in, f) =>
        run(in) { ev =>
          ev match {
            case OnNext(v) =>
              effect(ev)
            case OnError(e) if f.isDefinedAt(e) =>
              Try(effect(OnNext(f(e)))) match {
                case Success(x) =>
                  // recovery succeeded
                  RxResult.Continue
                case Failure(e) =>
                  effect(OnError(e))
              }
            case other =>
              effect(other)
          }
        }
      case RecoverWithOp(in, f) =>
        var toContinue: RxResult = RxResult.Continue
        var c1                   = Cancelable.empty
        val c2 = run(in) { ev =>
          ev match {
            case OnError(e) if f.isDefinedAt(e) =>
              c1.cancel
              Try(f(e)) match {
                case Success(recoverySource) =>
                  c1 = run(recoverySource) { ev =>
                    toContinue = effect(ev)
                    toContinue
                  }
                  toContinue
                case Failure(e) =>
                  effect(OnError(e))
              }
            case other =>
              effect(other)
          }
        }
        Cancelable { () =>
          c1.cancel; c2.cancel
        }
      case TapOnOp(in, f) =>
        val f0 = f.asInstanceOf[PartialFunction[Try[_], Unit]]
        run(in) { ev =>
          ev match {
            case OnNext(v) =>
              Try(f0.applyOrElse(Success(v), (_: Try[_]) => ())) match {
                case Success(value) =>
                  effect(ev)
                case Failure(e) =>
                  effect(OnError(e))
              }
            case OnError(e) =>
              Try(f0.applyOrElse(Failure(e), (_: Try[_]) => ())) match {
                case Success(value) =>
                  effect(ev)
                case Failure(e) =>
                  effect(OnError(e))
              }
            case _ =>
              effect(ev)
          }
        }
      case SingleOp(v) =>
        Try(effect(OnNext(v.eval))) match {
          case Success(c) =>
            effect(OnCompletion)
          case Failure(e) =>
            effect(OnError(e))
        }
        Cancelable.empty
      case SeqOp(inputList) =>
        var lastResult: RxResult = RxResult.Continue
        @tailrec
        def loop(lst: List[A]): Unit = {
          if (continuous || lastResult.toContinue) {
            lst match {
              case Nil =>
                lastResult = effect(OnCompletion)
              case head :: tail =>
                lastResult = effect(OnNext(head))
                loop(tail)
            }
          }
        }
        loop(inputList.eval.toList)
        // Stop reading the next element if cancelled
        Cancelable { () =>
          lastResult = RxResult.Stop
        }
      case source: RxSource[_] =>
        var toContinue     = true
        var c1: Cancelable = Cancelable.empty
        @tailrec
        def loop: Unit = {
          if (continuous || toContinue) {
            c1.cancel
            val evRx = source.next
            c1 = run(evRx) {
              case OnNext(ev: RxEvent) =>
                ev match {
                  case OnNext(v) =>
                    effect(OnNext(v.asInstanceOf[A]))
                  case other =>
                    toContinue = false
                    effect(other)
                }
              case OnCompletion =>
                // ok. Successfully received a single event from the source
                RxResult.Continue
              case other =>
                toContinue = false
                effect(other)
            }
            loop
          }
        }
        loop
        Cancelable.merge(
          c1,
          Cancelable { () =>
            toContinue = false
            source.add(OnError(new InterruptedException("cancelled")))
          }
        )
    }
  }

  /**
    * A base implementation for merging streams and generating tuples
    * @param input
    * @tparam A
    */
  private[rx] abstract class CombinedStream[A](input: Rx[A]) extends LogSupport {
    protected val size = input.parents.size

    protected val lastEvent: Array[Option[RxEvent]] = Array.fill(size)(None)
    private val c: Array[Cancelable]                = Array.fill(size)(Cancelable.empty)
    private val completed: AtomicBoolean            = new AtomicBoolean(false)

    protected def nextValue: Option[Seq[Any]]

    protected def update(index: Int, v: A): Unit

    protected def isCompleted: Boolean

    def run(effect: RxEvent => RxResult): Cancelable = {
      def emit: RxResult = {
        // Emit the tuple result.
        val toContinue = nextValue match {
          case None =>
            // Nothing to emit
            RxResult.Continue
          case Some(values) =>
            // Generate tuples from last values.
            // This code is a bit ad-hoc because there is no way to produce tuples from Seq[X] of lastValues
            values.size match {
              case 2 =>
                effect(OnNext((values(0), values(1)).asInstanceOf[A]))
              case 3 =>
                effect(OnNext((values(0), values(1), values(2)).asInstanceOf[A]))
              case 4 =>
                effect(OnNext((values(0), values(1), values(2), values(3)).asInstanceOf[A]))
              case 5 =>
                effect(OnNext((values(0), values(1), values(2), values(3), values(4)).asInstanceOf[A]))
              case 6 =>
                effect(OnNext((values(0), values(1), values(2), values(3), values(4), values(5)).asInstanceOf[A]))
              case 7 =>
                effect(
                  OnNext((values(0), values(1), values(2), values(3), values(4), values(5), values(6)).asInstanceOf[A])
                )
              case 8 =>
                effect(
                  OnNext(
                    (values(0), values(1), values(2), values(3), values(4), values(5), values(6), values(7))
                      .asInstanceOf[A]
                  )
                )
              case 9 =>
                effect(
                  OnNext(
                    (values(0), values(1), values(2), values(3), values(4), values(5), values(6), values(7), values(8))
                      .asInstanceOf[A]
                  )
                )
              case 10 =>
                effect(
                  OnNext(
                    (
                      values(0),
                      values(1),
                      values(2),
                      values(3),
                      values(4),
                      values(5),
                      values(6),
                      values(7),
                      values(8),
                      values(9)
                    ).asInstanceOf[A]
                  )
                )
              case other =>
                throw new NotImplementedError(s"combining 10+ more Rx operators is not yet supported: ${other}")
            }
        }
        toContinue
      }

      // Scan the last events and emit the next value or a completion event
      def processEvents(doEmit: Boolean): RxResult = {
        val errors = lastEvent.collect { case Some(e @ OnError(ex)) => ex }
        if (errors.isEmpty) {
          if (doEmit) {
            emit
          } else {
            if (isCompleted && completed.compareAndSet(false, true)) {
              trace(s"emit OnCompletion")
              effect(OnCompletion)
            } else {
              RxResult.Continue
            }
          }
        } else {
          // Report the completion event only once
          if (continuous || completed.compareAndSet(false, true)) {
            // If there are multiple exceptions, add them to the suppressed list
            val ex: Throwable = errors.reduce { (e1, e2) =>
              e1.addSuppressed(e2); e1
            }
            effect(OnError(ex))
          } else {
            RxResult.Continue
          }
        }
      }

      for (i <- 0 until size) {
        c(i) = runner.run(input.parents(i)) { e =>
          lastEvent(i) = Some(e)
          trace(s"c(${i}) ${e}")
          e match {
            case OnNext(v) =>
              update(i, v.asInstanceOf[A])
              processEvents(true)
            case _ =>
              processEvents(false)
          }
        }
      }

      processEvents(false)
      Cancelable { () =>
        c.foreach(_.cancel)
      }
    }
  }

  private class ZipStream[A](input: Rx[A]) extends CombinedStream(input) {
    private val lastValueBuffer: Array[Queue[A]] =
      Array.fill(size)(Queue.empty[A])

    override protected def nextValue: Option[Seq[Any]] = {
      if (lastValueBuffer.forall(_.nonEmpty)) {
        val values = for (i <- 0 until lastValueBuffer.size) yield {
          val (v, newQueue) = lastValueBuffer(i).dequeue
          lastValueBuffer(i) = newQueue
          v
        }
        Some(values)
      } else {
        None
      }
    }

    override protected def update(index: Int, v: A): Unit = {
      lastValueBuffer(index) = lastValueBuffer(index).enqueue(v)
    }

    override protected def isCompleted: Boolean = {
      !continuous && lastEvent.forall(_.isDefined)
    }
  }

  private def zip[A](input: Rx[A])(effect: RxEvent => RxResult): Cancelable = {
    new ZipStream(input).run(effect)
  }

  private class JoinStream[A](input: Rx[A]) extends CombinedStream(input) {
    private val lastValue: Array[Option[A]] = Array.fill(size)(None)

    override protected def nextValue: Option[Seq[Any]] = {
      if (lastValue.forall(_.nonEmpty)) {
        val values = for (i <- 0 until lastValue.size) yield {
          lastValue(i).get
        }
        Some(values)
      } else {
        None
      }
    }

    override protected def update(index: Int, v: A): Unit = {
      lastValue(index) = Some(v)
    }

    override protected def isCompleted: Boolean = {
      !continuous && lastEvent.forall(x => x.isDefined && x.get == OnCompletion)
    }
  }

  private def join[A](input: Rx[A])(effect: RxEvent => RxResult): Cancelable = {
    new JoinStream(input).run(effect)
  }

}
