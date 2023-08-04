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

import java.util.concurrent.atomic.AtomicInteger
import scala.util.{Failure, Success}

class RxSingleTest extends AirSpec {
  test("Rx.single") {
    val counter = new AtomicInteger(0)
    val rx = Rx
      .single(counter.incrementAndGet())
      .map(_ + 10)

    counter.get shouldBe 0
    rx.map {
      _ shouldBe 11
    }
  }

  test("Rx.single(exception)") {
    val counter = new AtomicInteger(0)
    val rx = Rx
      .single {
        counter.incrementAndGet()
        throw new IllegalStateException("test exception")
      }
      .map(_ => counter.incrementAndGet())

    counter.get shouldBe 0
    rx.transform {
      case Success(v) => fail("should not reach here")
      case Failure(e) =>
        counter.get() shouldBe 1
        e shouldMatch { case e: IllegalStateException =>
          e.getMessage shouldBe "test exception"
        }
    }
  }

  test("Rx.const") {
    val counter = new AtomicInteger(0)
    val rx = Rx
      .const(counter.incrementAndGet())
      .map(_ + 10)

    counter.get shouldBe 1
    rx.map {
      _ shouldBe 11
    }
  }

  test("Rx.const(exception)") {
    val counter = new AtomicInteger(0)
    val rx = Rx
      .const {
        counter.incrementAndGet()
        throw new IllegalStateException("test exception")
      }
      .map(_ => counter.incrementAndGet())

    counter.get shouldBe 1
    rx.transform {
      case Success(v) => fail("should not reach here")
      case Failure(e) =>
        counter.get() shouldBe 1
        e shouldMatch { case e: IllegalStateException =>
          e.getMessage shouldBe "test exception"
        }
    }
  }
}
