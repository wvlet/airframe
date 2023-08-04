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

class RxOpsTest extends AirSpec {

  test("From RxOption") {
    test("recoverWith RxOption") {
      Rx.some(1)
        .recoverWith { case e: Throwable => Rx.some(-1) }
        .map {
          _ shouldBe Some(1)
        }
    }

    test("flatMap to RxOption") {
      Rx.some(1)
        .flatMap(x => Rx.some(x + 1))
        .map {
          _ shouldBe Some(2)
        }
    }

    test("transformRx to RxOption") {
      Rx.some(1)
        .transformRx(x => Rx.some(x.getOrElse(-1)))
        .map {
          _ shouldBe Some(1)
        }
    }

    test("getOrElse RxOption") {
      Rx.some(1)
        .getOrElseRx(Rx.some(2))
        .map {
          _ shouldBe 1
        }

      Rx.none
        .getOrElseRx(Rx.some(-1))
        .map {
          _ shouldBe Some(-1)
        }
    }

    test("join") {
      Rx.some(1).join(Rx.some(2)).map {
        _ shouldBe (Some(1), Some(2))
      }
      Rx.some(1).join(Rx.some(2), Rx.some(3)).map {
        _ shouldBe (Some(1), Some(2), Some(3))
      }
      Rx.some(1).join(Rx.some(2), Rx.some(3), Rx.some(4)).map {
        _ shouldBe (Some(1), Some(2), Some(3), Some(4))
      }
      Rx.some(1).join(Rx.some(2), Rx.some(3), Rx.some(4), Rx.some(5)).map {
        _ shouldBe (Some(1), Some(2), Some(3), Some(4), Some(5))
      }
    }

    test("zip") {
      Rx.some(1).zip(Rx.some(2)).map {
        _ shouldBe (Some(1), Some(2))
      }
      Rx.some(1).zip(Rx.some(2), Rx.some(3)).map {
        _ shouldBe (Some(1), Some(2), Some(3))
      }
      Rx.some(1).zip(Rx.some(2), Rx.some(3), Rx.some(4)).map {
        _ shouldBe (Some(1), Some(2), Some(3), Some(4))
      }
      Rx.some(1).zip(Rx.some(2), Rx.some(3), Rx.some(4), Rx.some(5)).map {
        _ shouldBe (Some(1), Some(2), Some(3), Some(4), Some(5))
      }
    }
  }

  test("zip with RxOption") {
    Rx.const(1).zip(Rx.some(10)).map {
      _ shouldBe (1, Some(10))
    }
  }

  test("join with RxOption") {
    Rx.const(1).join(Rx.some(10)).map {
      _ shouldBe (1, Some(10))
    }
  }

}
