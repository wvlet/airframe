package wvlet.airframe.http.rx

object RxRunner {

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
  private[rx] def run[A, U](rx: Rx[A])(effect: A => U): Cancelable = {
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
      case RxOptionOp(in) =>
        run(in) {
          case Some(v) => effect(v)
          case None    =>
          // Do nothing
        }
      case NamedOp(input, name) =>
        run(input)(effect)
      case SingleOp(v) =>
        effect(v)
        Cancelable.empty
      case o: RxOptionVar[_] =>
        o.asInstanceOf[RxOptionVar[A]].foreach {
          case Some(v) => effect(v)
          case None    =>
          // Do nothing
        }
      case v: RxVar[_] =>
        v.asInstanceOf[RxVar[A]].foreach(effect)
    }
  }

}
