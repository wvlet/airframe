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

import scala.util.{Failure, Success}

class RxSideEffectTest extends AirSpec {
  test("Rx.tap(x)") {
    val observed = Seq.newBuilder[Int]
    Rx.fromSeq(Seq(1, 2, 3))
      .tap { v => observed += v }
      .lastOption
      .map { x =>
        x shouldBe 3
        observed.result() shouldBe Seq(1, 2, 3)
      }
  }

  test("Rx.tapOn(...)") {
    val observed        = Seq.newBuilder[Int]
    val observedFailure = Seq.newBuilder[Throwable]
    Rx.fromSeq(Seq(1, 2, 3))
      .map { x =>
        if (x == 2) throw new Exception("failed")
        x
      }
      .tapOn {
        case Success(v) =>
          observed += v
        case Failure(e) =>
          observedFailure += e
      }
      .recover {
        case e: Exception if e.getMessage == "failed" =>
          observed.result() shouldBe Seq(1)
          observedFailure.result().size shouldBe 1
      }
  }

  test("Rx.tapOnFailure(...)") {
    val observed = Seq.newBuilder[Throwable]
    Rx.fromSeq(Seq(1, 2, 3))
      .map { x =>
        if (x == 2) throw new Exception("failed")
        x
      }
      .tapOnFailure { e =>
        observed += e
      }
      .recover {
        case e: Exception if e.getMessage == "failed" =>
          val exList = observed.result()
          exList.size shouldBe 1
      }
  }

}
