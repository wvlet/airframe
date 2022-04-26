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

object RPCStatusTypeTest extends AirSpec {
  test("enumerate all types") {
    RPCStatusType.unapply("SUCCESS") shouldBe Some(RPCStatusType.SUCCESS)
    RPCStatusType.unapply("USER_ERROR") shouldBe Some(RPCStatusType.USER_ERROR)
    RPCStatusType.unapply("INTERNAL_ERROR") shouldBe Some(RPCStatusType.INTERNAL_ERROR)
    RPCStatusType.unapply("RESOURCE_EXHAUSTED") shouldBe Some(RPCStatusType.RESOURCE_EXHAUSTED)

    RPCStatusType.all.foreach { tpe =>
      RPCStatusType.unapply(tpe.name) shouldBe Some(tpe)
      RPCStatusType.unapply(tpe.name.toLowerCase) shouldBe Some(tpe)
      RPCStatusType.unapply(tpe.name.reverse) shouldBe None
    }
  }

  test("find from prefix") {
    RPCStatusType.ofPrefix('S') shouldBe RPCStatusType.SUCCESS
    RPCStatusType.ofPrefix('U') shouldBe RPCStatusType.USER_ERROR
    RPCStatusType.ofPrefix('I') shouldBe RPCStatusType.INTERNAL_ERROR
    RPCStatusType.ofPrefix('R') shouldBe RPCStatusType.RESOURCE_EXHAUSTED

    intercept[IllegalArgumentException] {
      RPCStatusType.ofPrefix('X')
    }
  }
}
