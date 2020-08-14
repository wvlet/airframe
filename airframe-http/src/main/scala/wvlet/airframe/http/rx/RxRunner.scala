package wvlet.airframe.http.rx

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

sealed trait RxEvent
case class OnNext(v: Any)        extends RxEvent
case class OnError(e: Throwable) extends RxEvent
case object OnCompletion         extends RxEvent

private[rx] object RxRunner {
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
      case ZipOp(left, right) =>
        var allReady: Boolean = false
        var v1: Any           = null
        var v2: Any           = null
        val c1 = runInternal(left) {
          case OnNext(x) =>
            v1 = x
            if (allReady) {
              effect(OnNext((v1, v2).asInstanceOf[A]))
            }
          case other =>
            effect(other)
        }
        val c2 = runInternal(right) {
          case OnNext(x) =>
            v2 = x
            if (allReady) {
              effect(OnNext((v1, v2).asInstanceOf[A]))
            }
          case other =>
            effect(other)
        }
        allReady = true
        effect(OnNext((v1, v2).asInstanceOf[A]))
        Cancelable { () => c1.cancel; c2.cancel }
      case Zip3Op(r1, r2, r3) =>
        var allReady: Boolean = false
        var v1: Any           = null
        var v2: Any           = null
        var v3: Any           = null
        val c1 = runInternal(r1) {
          case OnNext(x) =>
            v1 = x
            if (allReady) {
              effect(OnNext((v1, v2, v3).asInstanceOf[A]))
            }
          case other => effect(other)
        }
        val c2 = runInternal(r2) {
          case OnNext(x) =>
            v2 = x
            if (allReady) {
              effect(OnNext((v1, v2, v3).asInstanceOf[A]))
            }
          case other => effect(other)
        }
        val c3 = runInternal(r3) {
          case OnNext(x) =>
            v3 = x
            if (allReady) {
              effect(OnNext((v1, v2, v3).asInstanceOf[A]))
            }
          case other =>
            effect(other)
        }
        allReady = true
        effect(OnNext((v1, v2, v3).asInstanceOf[A]))
        Cancelable { () => c1.cancel; c2.cancel; c3.cancel }
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
        Try(effect(OnNext(v))) match {
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
        loop(inputList.toList)
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
    }
  }
}
