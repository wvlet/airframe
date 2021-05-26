package wvlet.airframe.rx

import wvlet.airspec.AirSpec
import java.util.concurrent.TimeUnit

object RxRunnerTest extends AirSpec {

  private val ex = new IllegalArgumentException("dummy")

  test("cancel") {
    test("concat") {
      val c = Rx.concat(Rx.const(1), Rx.const(2)).run { (event: Int) => }
      c.cancel
    }
  }

  test("Propagate errors as events") {
    test("last") {
      val rx            = Rx.sequence(1, 2).lastOption.map[Int] { x => throw ex }
      var c: Cancelable = Cancelable.empty
      RxRunner.run(rx) {
        case OnError(e) =>
          e shouldBeTheSameInstanceAs ex
        case OnCompletion =>
        // ok
        case other =>
          fail(s"should not reach here: ${other}")
      }
      c.cancel
    }

    test("cache") {
      val rx = Rx.const(1).map[Int] { x => throw ex }.cache
      var c  = Cancelable.empty
      c = RxRunner.run(rx) {
        case OnError(e) =>
          e shouldBeTheSameInstanceAs ex
        case OnCompletion =>
        // ok
        case other =>
          fail(s"should not reach here: ${other}")
      }
      c.cancel
    }

    test("take") {
      val rx = Rx.const(1).map { x => throw ex }.take(1)
      var c  = Cancelable.empty
      c = RxRunner.run(rx) {
        case OnError(e) =>
          e shouldBeTheSameInstanceAs ex
        case OnCompletion =>
        // ok
        case other =>
          fail(s"should not reach here: ${other}")
      }
      c.cancel

    }

    test("timer") {
      val rx = Rx.timer(1, TimeUnit.MILLISECONDS).map[Int] { x =>
        throw ex
      }
      var c = Cancelable.empty
      c = RxRunner.run(rx) {
        case OnError(e) =>
          e shouldBeTheSameInstanceAs ex
        case other =>
          fail(s"should not be here: ${other}")
      }
      // Wait a bit
      compat.scheduleOnce(10) {
        c.cancel
      }
    }

  }

}
