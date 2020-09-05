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
  test("timeIntervalMillis") {
    val counter = new AtomicInteger(0)
    val rx = Rx
      .interval(3, TimeUnit.MILLISECONDS)
      .take(3)
      .map { interval =>
        counter.incrementAndGet()
        info(s"schedule: ${counter.get}")
        interval
      }

    val s = Seq.newBuilder[Long]
    val c = rx.run { x =>
      s += x
    }
    try {
      if (isScalaJS) {
        pending("Need asynchronous test support")
      } else {
        while (counter.get() != 3) {
          // wait with a busy loop because Scala.js doesn't support Thread.sleep
        }
        val result = s.result()
        info(result)
        result.size shouldBe 3
      }
    } finally {
      c.cancel
    }
  }
}
