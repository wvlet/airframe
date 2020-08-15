package wvlet.airframe.http.rx

import java.util.concurrent.atomic.AtomicBoolean

import wvlet.airframe.control.MultipleExceptions
import wvlet.log.LogSupport

import scala.annotation.tailrec
import scala.collection.immutable.Queue
import scala.util.{Failure, Success, Try}
import Rx._

object RxRunner extends LogSupport {

  private val defaultRunner = new RxRunner(continuous = false)
  // Used for continuous RxVar evaluation (e.g., RxVar -> DOM rendering)
  private val continuousRunner = new RxRunner(continuous = true)

  def run[A, U](rx: Rx[A])(effect: RxEvent => U): Cancelable             = defaultRunner.run(rx)(effect)
  def runContinuously[A, U](rx: Rx[A])(effect: RxEvent => U): Cancelable = continuousRunner.run(rx)(effect)
}

class RxRunner(
    // If this value is true, evaluating Rx keeps reporting events after OnError or OnCompletion is observed
    continuous: Boolean
) extends LogSupport {

  /**
    * Build an executable chain of Rx operators. The resulting chain
    * will be registered as a subscriber to the root node (see RxVar.foreach). If the root value changes,
    * the effect code block will be executed.
    *
    * @param rx
    * @param effect
    * @tparam A
    * @tparam U
    */
  def run[A, U](rx: Rx[A])(effect: RxEvent => U): Cancelable = {
    rx match {
      case MapOp(in, f) =>
        var toContinue = true
        run(in) { ev =>
          if (continuous || toContinue) {
            ev match {
              case OnNext(v) =>
                Try(f.asInstanceOf[Any => A](v)) match {
                  case Success(x) => effect(OnNext(x))
                  case Failure(e) =>
                    toContinue = false
                    effect(OnError(e))
                }
              case other =>
                toContinue = false
                effect(other)
            }
          }
        }
      case fm @ FlatMapOp(in, f) =>
        var toContinue = true
        // This var is a placeholder to remember the preceding Cancelable operator, which will be updated later
        var c1 = Cancelable.empty
        val c2 = run(fm.input) { ev =>
          if (continuous || toContinue) {
            ev match {
              case OnNext(x) =>
                Try(fm.f.asInstanceOf[Any => Rx[A]](x)) match {
                  case Success(rxb) =>
                    // This code is necessary to properly cancel the effect if this operator is evaluated before
                    c1.cancel
                    c1 = run(rxb.asInstanceOf[Rx[A]]) {
                      case n @ OnNext(x) => effect(n)
                      case OnCompletion  => // skip the end of flatMap body stream
                      case ev @ OnError(e) =>
                        toContinue = false
                        effect(ev)
                    }
                  case Failure(e) =>
                    toContinue = false
                    effect(OnError(e))
                }
              case other =>
                toContinue = false
                effect(other)
            }
          }
        }
        Cancelable { () => c1.cancel; c2.cancel }
      case FilterOp(in, cond) =>
        var toContinue = true
        run(in) { ev =>
          if (continuous || toContinue) {
            ev match {
              case OnNext(x) =>
                Try(cond.asInstanceOf[A => Boolean](x.asInstanceOf[A])) match {
                  case Success(true) =>
                    effect(OnNext(x))
                  case Success(false) =>
                  // Skip unmatched element
                  case Failure(e) =>
                    toContinue = false
                    effect(OnError(e))
                }
              case other =>
                toContinue = false
                effect(other)
            }
          }
        }
      case ConcatOp(first, next) =>
        var c1 = Cancelable.empty
        val c2 = run(first) {
          case OnCompletion =>
            // Properly cancel the effect if this operator is evaluated before
            c1.cancel
            c1 = run(next)(effect)
            c1
          case other =>
            effect(other)
        }
        Cancelable { () => c1.cancel; c2.cancel }
      case LastOp(in) =>
        var last: Option[A] = None
        run(in) {
          case OnNext(v) =>
            last = Some(v.asInstanceOf[A])
          case err @ OnError(e) =>
            effect(err)
          case OnCompletion =>
            Try(effect(OnNext(last))) match {
              case Success(v) => effect(OnCompletion)
              case Failure(e) => effect(OnError(e))
            }
        }
      case z @ ZipOp(left, right) =>
        zip(z)(effect)
      case z @ Zip3Op(r1, r2, r3) =>
        zip(z)(effect)
      case RxOptionOp(in) =>
        run(in) {
          case OnNext(Some(v)) => effect(OnNext(v))
          case OnNext(None)    =>
          // do nothing for empty values
          case other => effect(other)
        }
      case NamedOp(input, name) =>
        run(input)(effect)
      case SingleOp(v) =>
        Try(effect(OnNext(v.eval))) match {
          case Success(c) => effect(OnCompletion)
          case Failure(e) => effect(OnError(e))
        }
        Cancelable.empty
      case SeqOp(inputList) =>
        var toContinue = true
        @tailrec
        def loop(lst: List[A]): Unit = {
          if (continuous || toContinue) {
            lst match {
              case Nil =>
                effect(OnCompletion)
              case head :: tail =>
                Try(effect(OnNext(head))) match {
                  case Success(x) => loop(tail)
                  case Failure(e) =>
                    effect(OnError(e))
                }
            }
          }
        }
        loop(inputList.eval.toList)
        // Stop reading the next element if cancelled
        Cancelable { () =>
          toContinue = false
        }
      case TryOp(e) =>
        e match {
          case Success(x) =>
            effect(OnNext(x))
          case Failure(e) =>
            effect(OnError(e))
        }
        Cancelable.empty
      case o: RxOptionVar[_] =>
        o.asInstanceOf[RxOptionVar[A]].foreach {
          case Some(v) => effect(OnNext(v))
          case None    =>
          // Do nothing
        }
      case v: RxVar[_] =>
        v.asInstanceOf[RxVar[A]].foreach { x => effect(OnNext(x)) }
      case RecoverOp(in, f) =>
        run(in) {
          case OnError(e) if f.isDefinedAt(e) =>
            Try(effect(OnNext(f(e)))) match {
              case Success(x) => effect(OnCompletion)
              case Failure(e) => effect(OnError(e))
            }
          case other =>
            effect(other)
        }
      case RecoverWithOp(in, f) =>
        var completed = false
        var c1        = Cancelable.empty
        val c2 = run(in) {
          case OnError(e) if f.isDefinedAt(e) =>
            c1.cancel
            Try(f(e)) match {
              case Success(recoverySource) =>
                c1 = run(recoverySource)(effect)
              case Failure(e) =>
                completed = true
                effect(OnError(e))
            }
          case e if e.isLastEvent && completed =>
          // Skip reporting the completion event
          case other =>
            effect(other)
        }
        Cancelable { () => c1.cancel; c2.cancel }
    }
  }

  private def zip[A, U](input: Rx[A])(effect: RxEvent => U): Cancelable = {
    val size                              = input.parents.size
    val lastValueBuffer: Array[Queue[A]]  = Array.fill(size)(Queue.empty[A])
    val lastEvent: Array[Option[RxEvent]] = Array.fill(size)(None)
    val c: Array[Cancelable]              = Array.fill(size)(Cancelable.empty)

    val completed: AtomicBoolean = new AtomicBoolean(false)
    def emit: Unit = {
      // Emit the tuple result.
      if (lastValueBuffer.forall(_.nonEmpty)) {
        val values = for (i <- 0 until lastValueBuffer.size) yield {
          val (v, newQueue) = lastValueBuffer(i).dequeue
          lastValueBuffer(i) = newQueue
          v
        }
        // Generate tuples from last values.
        // This code is a bit ad-hoc because there is no way to produce tuples from Seq[X] of lastValues
        lastValueBuffer.size match {
          case 2 =>
            // For zip2
            trace(s"emit :${lastValueBuffer.mkString(", ")}")
            effect(OnNext((values(0), values(1)).asInstanceOf[A]))
          case 3 =>
            // For zip3
            effect(OnNext((values(0), values(1), values(2)).asInstanceOf[A]))
          case _ => ???
        }
      }
    }

    // Scan the last events and emit the next value or a completion event
    def processEvents(doEmit: Boolean) = {
      val errors = lastEvent.collect { case Some(e @ OnError(ex)) => ex }
      if (errors.isEmpty) {
        if (doEmit) {
          emit
        } else {
          if (!continuous && lastEvent.forall(_.isDefined) && completed.compareAndSet(false, true)) {
            trace(s"emit OnCompletion")
            effect(OnCompletion)
          }
        }
      } else {
        // Report the completion event only once
        if (continuous || completed.compareAndSet(false, true)) {
          if (errors.size == 1) {
            effect(OnError(errors(0)))
          } else {
            effect(OnError(MultipleExceptions(errors.toSeq)))
          }
        }
      }
    }

    for (i <- 0 until size) {
      c(i) = run(input.parents(i)) { e =>
        lastEvent(i) = Some(e)
        trace(s"c(${i}) ${e}")
        e match {
          case OnNext(v) =>
            lastValueBuffer(i) = lastValueBuffer(i).enqueue(v.asInstanceOf[A])
            processEvents(true)
          case _ =>
            processEvents(false)
        }
      }
    }

    processEvents(false)
    Cancelable { () => c.foreach(_.cancel) }
  }

}
