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

import scala.util.{Failure, Success, Try}

class RxTransformTest extends AirSpec {

  test("Rx.transform") {
    Rx.single("success")
      .transform {
        case Success(x) => x
        case Failure(e) => "recovered"
      }.map {
        case "success" =>
        // ok. do nothing
        case other =>
          fail(s"Unexpected: ${other}")
      }
  }

  test("Rx.transform failure") {
    Rx.exception(new Exception("failed"))
      .transform {
        case Success(x) => x
        case Failure(e) => "recovered"
      }.map {
        case "recovered" =>
        // ok. do nothing
        case other =>
          fail(s"Unexpected: ${other}")
      }
  }

  test("Rx.transformRx") {
    Rx.single("success")
      .transformRx {
        case Success(x) => Rx.single(x)
        case Failure(e) => Rx.single("recovered")
      }.map {
        case "success" | "recovered" =>
        // ok. do nothing
        case other =>
          fail(s"Unexpected: ${other}")
      }
  }
  test("Rx.transformRx Try") {
    Rx.fromTry(Success("success"))
      .transformRx {
        case Success(x) => Rx.single(x)
        case Failure(e) => Rx.single("recovered")
      }.map {
        case "success" | "recovered" =>
        // ok. do nothing
        case other =>
          fail(s"Unexpected: ${other}")
      }
  }

  test("Rx.transformRx failure") {
    Rx.exception(new Exception("failed"))
      .transformRx {
        case Success(x) => Rx.single(x)
        case Failure(e) => Rx.single("recovered")
      }.map {
        case "recovered" =>
        // ok. do nothing
        case other =>
          fail(s"Unexpected: ${other}")
      }
  }

  test("Rx.transformRx Try(failure)") {
    Rx.fromTry(Failure(new Exception("failed")))
      .transformRx {
        case Success(x) => Rx.single(x)
        case Failure(e) => Rx.single("recovered")
      }.map {
        case "recovered" =>
        // ok. do nothing
        case other =>
          fail(s"Unexpected: ${other}")
      }
  }
}
