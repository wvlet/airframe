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
package examples

import wvlet.airframe.spec.AirSpec
import wvlet.airframe.spec.spi.AssertionFailure

/**
  *
  */
class ShouldBeTest extends AirSpec {

  def `support shouldBe matcher`: Unit = {
    1 shouldBe 1
  }

  def `support shouldNotBe matcher`: Unit = {
    1 shouldNotBe 2
  }

  def `throw an assertion error for invalid matches`: Unit = {
    intercept[AssertionFailure] {
      1 shouldBe 3
    }

    intercept[AssertionFailure] {
      1 shouldNotBe 1
    }

    intercept[AssertionFailure] {
      Seq(1) shouldBe Seq(2)
    }

    intercept[AssertionFailure] {
      Map(1 -> "apple", 2 -> "banana") shouldBe Map(1 -> "apple", 2 -> "orange")
    }
  }

  def `support collection matchers`: Unit = {
    Seq.empty shouldBe Seq.empty
    Seq(1) shouldBe List(1)
    Map(1 -> "apple", 2 -> "banana") shouldBe Map(1 -> "apple", 2 -> "banana")
  }

  def `support array matchers`: Unit = {

    Array(1, 2, 3) shouldBe Array(1, 2, 3)
    Array(1.toShort) shouldBe Array(1.toShort)
    Array("hello") shouldBe Array("hello")
  }

  def `throw assertion failures for invalid array matches`: Unit = {
    intercept[AssertionFailure] {
      Array(1, 2, 3) shouldBe Array(1, 2, 4)
    }

    intercept[AssertionFailure] {
      Array("hello") shouldBe Array("hello-x")
    }
  }

}
