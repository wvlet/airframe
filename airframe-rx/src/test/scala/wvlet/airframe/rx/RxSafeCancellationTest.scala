package wvlet.airframe.rx

import wvlet.airspec.AirSpec

class RxSafeCancellationTest extends AirSpec {

  test("demonstrate self-cancellation scenario") {
    test("cancel within operator function should work safely") {
      val results = scala.collection.mutable.ListBuffer[Int]()
      val rx = Rx.sequence(1, 2, 3, 4, 5)
      
      var c = Cancelable.empty
      c = rx.map { x =>
        results += x
        if (x == 3) {
          // User wants to cancel the stream at this point
          c.cancel
        }
        x * 2
      }.run()

      // We should be able to process up to the point of cancellation
      // and not process further elements
      assert(results.size <= 3)
      results.contains(1) shouldBe true
      results.contains(2) shouldBe true
      results.contains(3) shouldBe true
    }
    
    test("request completion within operator should work") {
      val results = scala.collection.mutable.ListBuffer[Int]()
      val rx = Rx.sequence(1, 2, 3, 4, 5)
      
      // What we'd like: a way to request completion safely
      var requestCompletion: () => Unit = () => {}
      
      val c = rx.map { x =>
        results += x
        if (x == 3) {
          // Request completion instead of cancelling 
          requestCompletion()
        }
        x * 2
      }.run()
      
      // After implementing the safe completion mechanism,
      // we should process 1, 2, 3 but not 4, 5
      assert(results.size <= 3)
    }
  }

}