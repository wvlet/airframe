package wvlet.airframe.http.rx

import java.util.concurrent.atomic.AtomicBoolean

import wvlet.airframe.control.MultipleExceptions
import wvlet.log.LogSupport

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

sealed trait RxEvent {
  def isLastEvent: Boolean
  def isError: Boolean
}
case class OnNext(v: Any) extends RxEvent {
  override def isLastEvent: Boolean = false
  override def isError: Boolean     = false
}
case class OnError(e: Throwable) extends RxEvent {
  override def isLastEvent: Boolean = true
  override def isError: Boolean     = true
}
case object OnCompletion extends RxEvent {
  override def isLastEvent: Boolean = true
  override def isError: Boolean     = false
}

private[rx] object RxRunner extends LogSupport {
  import Rx._

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
  private[rx] def runInternal[A, U](rx: Rx[A])(effect: RxEvent => U): Cancelable = {
    rx match {
      case MapOp(in, f) =>
        runInternal(in) {
          case OnNext(v) =>
            Try(f.asInstanceOf[Any => A](v)) match {
              case Success(x) => effect(OnNext(x))
              case Failure(e) => effect(OnError(e))
            }
          case other =>
            effect(other)
        }
      case FlatMapOp(in, f) =>
        // This var is a placeholder to remember the preceding Cancelable operator, which will be updated later
        var c1 = Cancelable.empty
        val c2 = runInternal(in) {
          case OnNext(x) =>
            Try(f.asInstanceOf[Any => Rx[A]](x)) match {
              case Success(rxb) =>
                // This code is necessary to properly cancel the effect if this operator is evaluated before
                c1.cancel
                c1 = runInternal(rxb.asInstanceOf[Rx[A]])(effect)
              case Failure(e) =>
                effect(OnError(e))
            }
          case other =>
            effect(other)
        }
        Cancelable { () => c1.cancel; c2.cancel }
      case FilterOp(in, cond) =>
        runInternal(in) {
          case OnNext(x) =>
            Try(cond.asInstanceOf[A => Boolean](x.asInstanceOf[A])) match {
              case Success(true) =>
                effect(OnNext(x))
              case Success(false) =>
              // Skip unmatched element
              case Failure(e) =>
                effect(OnError(e))
            }
          case other =>
            effect(other)
        }
      case ConcatOp(first, next) =>
        var c1 = Cancelable.empty
        val c2 = runInternal(first) {
          case OnCompletion =>
            // Properly cancel the effect if this operator is evaluated before
            c1.cancel
            c1 = runInternal(next)(effect)
            c1
          case other =>
            effect(other)
        }
        Cancelable { () => c1.cancel; c2.cancel }
      case LastOp(in) =>
        var last: Option[A] = None
        runInternal(in) {
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
        runInternal(in) {
          case OnNext(Some(v)) => effect(OnNext(v))
          case OnNext(None)    =>
          // do nothing for empty values
          case other => effect(other)
        }
      case NamedOp(input, name) =>
        runInternal(input)(effect)
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
          if (toContinue) {
            lst match {
              case Nil =>
                effect(OnCompletion)
              case head :: tail =>
                Try(effect(OnNext(head))) match {
                  case Success(x) => loop(tail)
                  case Failure(e) => effect(OnError(e))
                }
            }
          }
        }
        loop(inputList.eval.toList)
        // Stop reading the next element if cancelled
        Cancelable { () =>
          toContinue = false
        }
      case o: RxOptionVar[_] =>
        o.asInstanceOf[RxOptionVar[A]].foreach {
          case Some(v) => effect(OnNext(v))
          case None    =>
          // Do nothing
        }
      case v: RxVar[_] =>
        v.asInstanceOf[RxVar[A]].foreach { x => effect(OnNext(x)) }
      case RecoverOp(in, f) =>
        runInternal(in) {
          case OnCompletion =>
          case OnError(e) if f.isDefinedAt(e) =>
            effect(OnNext(f(e)))
          case other =>
            effect(other)
        }
      case RecoverWithOp(in, f) =>
        var c1 = Cancelable.empty
        val c2 = runInternal(in) {
          case OnCompletion =>
          case OnError(e) if f.isDefinedAt(e) =>
            c1.cancel
            Try(f(e)) match {
              case Success(rxb) =>
                c1 = runInternal(rxb)(effect)
              case Failure(e) =>
                effect(OnError(e))
            }
          case other =>
            effect(other)
        }
        Cancelable { () => c1.cancel; c2.cancel }
    }
  }

  private def zip[A, U](input: Rx[A])(effect: RxEvent => U): Cancelable = {
    val size                               = input.parents.size
    val lastValues: Array[Option[A]]       = Array.fill(size)(None)
    val lastEvents: Array[Option[RxEvent]] = Array.fill(size)(None)
    val c: Array[Cancelable]               = Array.fill(size)(Cancelable.empty)

    val completed: AtomicBoolean = new AtomicBoolean(false)
    def emit: Unit = {
      // Emit the tuple result. This code is a bit ad-hoc because there is no way to produce tuples from Seq[X]
      lastValues match {
        case Array(Some(v1), Some(v2)) =>
          // For zip2
          trace(s"emit :${lastValues.mkString(", ")}")
          effect(OnNext((v1, v2).asInstanceOf[A]))
        case Array(Some(v1), Some(v2), Some(v3)) =>
          // For zip3
          effect(OnNext((v1, v2, v3).asInstanceOf[A]))
        case _ =>
      }
    }

    // Scan the last events and emit the next value or a completion event
    def processEvents(doEmit: Boolean) = {
      val errors = lastEvents.collect { case Some(e @ OnError(ex)) => ex }
      if (errors.isEmpty) {
        if (doEmit) {
          emit
        } else {
          if (lastEvents.forall(_.isDefined) && completed.compareAndSet(false, true)) {
            trace(s"emit OnCompletion")
            effect(OnCompletion)
          }
        }
      } else {
        // Report the completion event only once
        if (completed.compareAndSet(false, true)) {
          if (errors.size == 1) {
            effect(OnError(errors(0)))
          } else {
            effect(OnError(MultipleExceptions(errors.toSeq)))
          }
        }
      }
    }

    for (i <- 0 until size) {
      c(i) = runInternal(input.parents(i)) { e =>
        lastEvents(i) = Some(e)
        trace(s"c(${i}) ${e}")
        e match {
          case OnNext(v) =>
            lastValues(i) = Some(v.asInstanceOf[A])
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
