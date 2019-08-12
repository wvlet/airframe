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
    Array(1, 2, 3) shouldBe defined
    Array(1, 2, 3) shouldNotBe empty

    Array(1L, 2L, 3L) shouldBe Array(1L, 2L, 3L)
    Array(1.toShort) shouldBe Array(1.toShort)
    Array(1.0f) shouldBe Array(1.0f)
    Array(1.0) shouldBe Array(1.0)
    Array('a') shouldBe Array('a')
    Array(true, false) shouldBe Array(true, false)
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

  def `support Option matchers`: Unit = {
    Some(1) shouldBe defined
    Some(1) shouldNotBe empty
    None shouldBe empty
    None shouldNotBe defined
    Option(null) shouldBe empty
    Option(null) shouldNotBe defined

    intercept[AssertionFailure] {
      Some(1) shouldBe empty
    }

    intercept[AssertionFailure] {
      Some(1) shouldNotBe defined
    }
  }

  def `support defined/empty for Seq[A]` : Unit = {
    Seq() shouldBe empty
    Seq() shouldNotBe defined

    Seq(1) shouldBe defined
    Seq(1) shouldNotBe empty

    Map() shouldBe empty

    Array(1) shouldBe defined
    Array(1) shouldNotBe empty
  }

  def `support tuples` = {
    (1, 2, 3) shouldBe (1, 2, 3)
    (1, 2, 3) shouldNotBe (1, 3, 2)
    (1, 'a') shouldBe (1, 'a')
    (2, 'a') shouldNotBe (2, 'b')
  }
}
