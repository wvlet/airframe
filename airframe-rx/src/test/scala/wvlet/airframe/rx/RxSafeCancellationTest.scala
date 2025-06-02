package wvlet.airframe.rx

import wvlet.airspec.AirSpec

class RxSafeCancellationTest extends AirSpec {

  test("mapWithCompletion should allow safe completion signaling") {
    val results = scala.collection.mutable.ListBuffer[Int]()
    val rx      = Rx.sequence(1, 2, 3, 4, 5)

    val c = rx
      .mapWithCompletion { x =>
        results += x
        if (x == 3) {
          // Request completion by returning None
          None
        } else {
          Some(x * 2)
        }
      }.run()

    // Should process 1, 2, 3 but not 4, 5
    results.toList shouldBe List(1, 2, 3)
  }

  test("mapWithCompletion should work with early completion") {
    val results = scala.collection.mutable.ListBuffer[Int]()

    val c = Rx
      .sequence(1, 2, 3, 4, 5).mapWithCompletion { x =>
        if (x == 2) {
          None // Complete immediately when seeing 2
        } else {
          results += x
          Some(x * 10)
        }
      }.run()

    // Should only process 1, then complete when seeing 2
    results.toList shouldBe List(1)
  }

  test("mapWithCompletion should handle empty sequence") {
    val results = scala.collection.mutable.ListBuffer[Int]()

    val c = Rx
      .empty[Int].mapWithCompletion { x =>
        results += x
        Some(x * 2)
      }.run()

    // Should have no results for empty sequence
    results.toList shouldBe List()
  }

  test("mapWithCompletion should propagate errors") {
    val ex = new RuntimeException("test error")

    val thrown = intercept[RuntimeException] {
      Rx.sequence(1, 2, 3).mapWithCompletion { x =>
          if (x == 2) {
            throw ex
          }
          Some(x * 2)
        }.run()
    }

    thrown shouldBe ex
  }

  test("flatMapWithCompletion should allow safe completion signaling") {
    val results = scala.collection.mutable.ListBuffer[Int]()
    val rx      = Rx.sequence(1, 2, 3, 4, 5)

    val c = rx
      .flatMapWithCompletion { x =>
        results += x
        if (x == 3) {
          // Request completion by returning None
          None
        } else {
          Some(Rx.single(x * 10))
        }
      }.run()

    // Should process 1, 2, 3 but not 4, 5
    results.toList shouldBe List(1, 2, 3)
  }

  test("flatMapWithCompletion should work with early completion") {
    val results = scala.collection.mutable.ListBuffer[Int]()

    val c = Rx
      .sequence(1, 2, 3, 4, 5).flatMapWithCompletion { x =>
        if (x == 2) {
          None // Complete immediately when seeing 2
        } else {
          results += x
          Some(Rx.single(x * 100))
        }
      }.run()

    // Should only process 1, then complete when seeing 2
    results.toList shouldBe List(1)
  }

}
