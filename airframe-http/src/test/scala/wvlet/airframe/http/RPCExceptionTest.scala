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

object RPCExceptionTest extends AirSpec {
  test("enumerate all error types") {
    ErrorType.unapply("USER_ERROR") shouldBe Some(ErrorType.USER_ERROR)
    ErrorType.unapply("INTERNAL_ERROR") shouldBe Some(ErrorType.INTERNAL_ERROR)
    ErrorType.unapply("RESOURCE_ERROR") shouldBe Some(ErrorType.RESOURCE_ERROR)

    ErrorType.all.foreach { tpe =>
      ErrorType.unapply(tpe.name) shouldBe Some(tpe)
      ErrorType.unapply(tpe.name.toLowerCase) shouldBe Some(tpe)
      ErrorType.unapply(tpe.name.reverse) shouldBe None
    }
  }

  test("report RPC error") {}

  test("update exception instance") {
    val ex1 = RPCException(StandardErrorCode.SYNTAX_ERROR, "syntax error")
    val ex2 = ex1.withMessage("updated error message")

    info(ex1)
    info(ex2)
  }

  test("standard error code") {}

}
