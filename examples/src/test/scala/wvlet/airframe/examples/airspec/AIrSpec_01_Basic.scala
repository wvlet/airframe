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
package wvlet.airframe.examples.airspec

import wvlet.airspec.AirSpec

/**
  */
object AirSpec_01_Basic extends AirSpec {
  def testWithAssertion: Unit = {
    assert(1 == 1)
    assert("hello" == "hello")
  }

  def testWithShouldBe: Unit = {
    Seq().size shouldBe 0
    Seq() shouldBe empty
  }

  def testObjectEquality: Unit = {
    val s1 = new String("hello")
    val s2 = s1
    val s3 = new String("hello")

    s1 shouldBe s2
    s1 shouldBeTheSameInstanceAs s2
    s1 shouldNotBeTheSameInstanceAs s3
  }
}
