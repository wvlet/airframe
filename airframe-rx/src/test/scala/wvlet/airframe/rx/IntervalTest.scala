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
import java.util.concurrent.atomic.AtomicInteger

import wvlet.airspec.AirSpec

/**
  */
class IntervalTest extends AirSpec {
  private def pendingInScalaJSAndScalaNative = if (isScalaJS) {
    pending("Async test is required")
  } else if (isScalaNative) {
    pending("Timer is not yet supported in Scala Native")
  }

  test("timeIntervalMillis") {
    pendingInScalaJSAndScalaNative

    val counter = new AtomicInteger(0)
    val rx = Rx
      .interval(3, TimeUnit.MILLISECONDS)
      .take(3)
      .map { interval =>
        debug(s"schedule: ${counter.get}")
        interval
      }

    val s = Seq.newBuilder[Long]
    val c = rx.run { x =>
      s += x
      counter.incrementAndGet()
    }
    try {
      compat.scheduleOnce(200) {
        val result = s.result()
        debug(result)
        result.size shouldBe 3
      }
    } finally {
      c.cancel
    }
  }

  test("timer/delay") {
    pendingInScalaJSAndScalaNative
    val counter = new AtomicInteger(0)
    val rx = Rx
      .delay(1, TimeUnit.MILLISECONDS)
      .map { interval =>
        interval
      }

    val s = Seq.newBuilder[Long]
    val c = rx.run { x =>
      s += x
      counter.incrementAndGet()
    }
    try {
      compat.scheduleOnce(200) {
        val result = s.result()
        result.size shouldBe 1
      }
    } finally {
      c.cancel
    }

  }

  test("throttleFirst") {
    pendingInScalaJSAndScalaNative
    val rx = Rx
      .sequence(1, 2, 3, 4, 5, 6)
      .throttleFirst(10000, TimeUnit.MILLISECONDS)

    val counter = new AtomicInteger(0)
    val s       = Seq.newBuilder[Long]
    val c = rx.run { x =>
      counter.incrementAndGet()
      s += x
    }

    while (counter.get() != 1) {}
    c.cancel
    s shouldBe Seq(1)
  }

  test("throttleLast") {
    pendingInScalaJSAndScalaNative

    flaky {
      val rx =
        Rx.sequence(1, 2, 3)
          .throttleLast(500, TimeUnit.MILLISECONDS)
      val counter = new AtomicInteger(0)
      val s       = Seq.newBuilder[Long]
      val c = rx.run { x =>
        counter.incrementAndGet()
        s += x
      }
      while (counter.get() != 1) {}
      c.cancel
      s shouldBe Seq(3)
    }
  }

  test("throttleLast of empty seq") {
    pendingInScalaJSAndScalaNative
    val rx = Rx.fromSeq(Seq.empty[Int]).throttleLast(1, TimeUnit.MILLISECONDS)
    val c  = rx.run { x => }
    compat.scheduleOnce(100) {
      c.cancel
    }
  }
}
