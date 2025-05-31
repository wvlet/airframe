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
 * Test for Rx.delay(...) operation
 */
class RxDelayTest extends AirSpec {
  private def pendingInScalaJSAndScalaNative = if (isScalaJS) {
    pending("Async test is required")
  } else if (isScalaNative) {
    pending("Timer is not yet supported in Scala Native")
  }

  test("delay with single value") {
    pendingInScalaJSAndScalaNative

    val observed = Seq.newBuilder[Int]
    val startTime = System.currentTimeMillis()
    
    val rx = Rx.single(42)
      .delay(50, TimeUnit.MILLISECONDS)
    
    val c = rx.run { x =>
      observed += x
      val elapsed = System.currentTimeMillis() - startTime
      assert(elapsed >= 50L, s"Expected delay of at least 50ms, but took ${elapsed}ms")
    }
    
    compat.scheduleOnce(200) {
      val result = observed.result()
      result shouldBe Seq(42)
      c.cancel
    }
  }

  test("delay with multiple values preserves order") {
    pendingInScalaJSAndScalaNative

    val observed = Seq.newBuilder[Int]
    val timestamps = Seq.newBuilder[Long]
    val startTime = System.currentTimeMillis()
    
    val rx = Rx.fromSeq(Seq(1, 2, 3))
      .delay(30, TimeUnit.MILLISECONDS)
    
    val c = rx.run { x =>
      observed += x
      timestamps += (System.currentTimeMillis() - startTime)
    }
    
    compat.scheduleOnce(300) {
      val result = observed.result()
      val times = timestamps.result()
      
      result shouldBe Seq(1, 2, 3)
      // Each value should be delayed by at least 30ms
      times.foreach { time =>
        assert(time >= 30L, s"Expected delay of at least 30ms, but took ${time}ms")
      }
      c.cancel
    }
  }

  test("delay with error propagation") {
    pendingInScalaJSAndScalaNative

    val observedValues = Seq.newBuilder[Int]
    val observedErrors = Seq.newBuilder[Throwable]
    
    val rx = Rx.fromSeq(Seq(1, 2, 3))
      .map { x =>
        if (x == 2) throw new RuntimeException("test error")
        x
      }
      .delay(20, TimeUnit.MILLISECONDS)
    
    val c = rx.run { x =>
      observedValues += x
    }
    
    compat.scheduleOnce(200) {
      val values = observedValues.result()
      // Should get value 1, then error occurs
      values shouldBe Seq(1)
      c.cancel
    }
  }

  test("delay works with chained operations") {
    pendingInScalaJSAndScalaNative

    val observed = Seq.newBuilder[Int]
    
    val rx = Rx.fromSeq(Seq(1, 2, 3))
      .delay(20, TimeUnit.MILLISECONDS)
      .map(_ * 2)
      .filter(_ > 2)
    
    val c = rx.run { x =>
      observed += x
    }
    
    compat.scheduleOnce(200) {
      val result = observed.result()
      result shouldBe Seq(4, 6)
      c.cancel
    }
  }

  test("delay default time unit is milliseconds") {
    pendingInScalaJSAndScalaNative

    val observed = Seq.newBuilder[Int]
    val startTime = System.currentTimeMillis()
    
    val rx = Rx.single(42).delay(50) // Should default to milliseconds
    
    val c = rx.run { x =>
      observed += x
      val elapsed = System.currentTimeMillis() - startTime
      assert(elapsed >= 50L, s"Expected delay of at least 50ms, but took ${elapsed}ms")
    }
    
    compat.scheduleOnce(200) {
      val result = observed.result()
      result shouldBe Seq(42)
      c.cancel
    }
  }
}