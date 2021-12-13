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
package wvlet.airframe.http

import wvlet.airspec.AirSpec

class RPCExceptionTest extends AirSpec {

  test("enumerate all error types") {
    ErrorType.unapply("USER_ERROR") shouldBe Some(ErrorType.USER_ERROR)
    ErrorType.unapply("INTERNAL_ERROR") shouldBe Some(ErrorType.INTERNAL_ERROR)
    ErrorType.unapply("EXTERNAL_ERROR") shouldBe Some(ErrorType.EXTERNAL_ERROR)

    ErrorType.all.foreach { tpe =>
      ErrorType.unapply(tpe.name) shouldBe Some(tpe)
      ErrorType.unapply(tpe.name.toLowerCase) shouldBe Some(tpe)
      ErrorType.unapply(tpe.name.reverse) shouldBe None
    }
  }
}
