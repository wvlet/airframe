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

import wvlet.airspec.AirSpec

import java.util.concurrent.TimeUnit

class RxCacheTest extends AirSpec {
  private def evalStream(rx: Rx[_]): Seq[RxEvent] = {
    val events = Seq.newBuilder[RxEvent]
    val c      = RxRunner.runContinuously(rx)(events += _)
    c.cancel
    events.result()
  }

  test("cache") {
    val v                      = Rx.variable(1)
    val rx: RxStreamCache[Int] = v.map(x => x * 10).cache
    rx.getCurrent shouldBe empty
    evalStream(rx) shouldBe Seq(OnNext(10))

    v := 2
    evalStream(rx) shouldBe Seq(
      OnNext(10),
      OnNext(20)
    )
    rx.getCurrent shouldBe Some(20)

    val events2 = Seq.newBuilder[RxEvent]
    v := 3
    val c2 = RxRunner.runContinuously(rx)(events2 += _)
    v := 4
    c2.cancel
    events2.result() shouldBe Seq(
      OnNext(20),
      OnNext(30),
      OnNext(40)
    )

    rx.getCurrent shouldBe Some(40)
  }

  test("cache.expireAfterWrite") {
    val ticker = Ticker.manualTicker
    val v      = Rx.variable(1)
    val rx     = v.map(x => x * 10).cache.expireAfterWrite(1, TimeUnit.MINUTES).withTicker(ticker)
    rx.getCurrent shouldBe empty
    evalStream(rx) shouldBe Seq(OnNext(10))
    rx.getCurrent shouldBe Some(10)

    v := 2
    evalStream(rx) shouldBe Seq(
      OnNext(10),
      OnNext(20)
    )
    rx.getCurrent shouldBe Some(20)

    v := 3
    rx.getCurrent shouldBe Some(20)
    // Force expiration of the cache
    ticker.advance(1, TimeUnit.MINUTES)
    evalStream(rx) shouldBe Seq(
      OnNext(30)
    )
    rx.getCurrent shouldBe Some(30)
  }

  test("cache for option") {
    val v  = Rx.optionVariable(Some(1))
    val rx = v.cache
    rx.getCurrent shouldBe empty
    evalStream(rx) shouldBe Seq(OnNext(Some(1)))
    rx.getCurrent shouldBe Some(1)

    v := Some(2)
    rx.getCurrent shouldBe Some(1)
    evalStream(rx) shouldBe Seq(
      OnNext(Some(1)),
      OnNext(Some(2))
    )
    rx.getCurrent shouldBe Some(2)
  }

  test("cache expiration for option") {
    val ticker = Ticker.manualTicker
    val v      = Rx.optionVariable(Some(1))
    val rx     = v.cache.expireAfterWrite(1, TimeUnit.MINUTES).withTicker(ticker)
    rx.getCurrent shouldBe empty
    evalStream(rx) shouldBe Seq(OnNext(Some(1)))
    rx.getCurrent shouldBe Some(1)

    v := Some(2)
    rx.getCurrent shouldBe Some(1)

    // Force expiration of the cache
    ticker.advance(1, TimeUnit.MINUTES)
    evalStream(rx) shouldBe Seq(
      OnNext(Some(2))
    )
    rx.getCurrent shouldBe Some(2)
  }
}
