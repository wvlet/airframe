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

object RPCErrorTypeTest extends AirSpec {
  test("enumerate all error types") {
    RPCErrorType.unapply("USER_ERROR") shouldBe Some(RPCErrorType.USER_ERROR)
    RPCErrorType.unapply("INTERNAL_ERROR") shouldBe Some(RPCErrorType.INTERNAL_ERROR)
    RPCErrorType.unapply("RESOURCE_ERROR") shouldBe Some(RPCErrorType.RESOURCE_ERROR)

    RPCErrorType.all.foreach { tpe =>
      RPCErrorType.unapply(tpe.name) shouldBe Some(tpe)
      RPCErrorType.unapply(tpe.name.toLowerCase) shouldBe Some(tpe)
      RPCErrorType.unapply(tpe.name.reverse) shouldBe None
    }
  }
}