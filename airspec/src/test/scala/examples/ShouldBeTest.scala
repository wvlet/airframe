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

import wvlet.airspec.spi.AssertionFailure
import wvlet.airspec.AirSpec

/**
  */
class ShouldBeTest extends AirSpec {
  test("support shouldBe matcher") {
    1 shouldBe 1
  }

  test("support shouldNotBe matcher") {
    1 shouldNotBe 2
  }

  test("throw an assertion error for invalid matches") {
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

  test("support collection matchers") {
    Seq.empty shouldBe Seq.empty
    Seq.empty shouldBe empty
    Seq(1) shouldBe List(1)
    Map(1 -> "apple", 2 -> "banana") shouldBe Map(1 -> "apple", 2 -> "banana")
  }

  test("support array matchers") {
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

  test("throw assertion failures for invalid array matches") {
    intercept[AssertionFailure] {
      Array(1, 2, 3) shouldBe Array(1, 2, 4)
    }

    intercept[AssertionFailure] {
      Array("hello") shouldBe Array("hello-x")
    }
  }

  test("support Option matchers") {
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

  test("support defined/empty for Seq[A]") {
    Seq() shouldBe empty
    Seq() shouldNotBe defined

    Seq(1) shouldBe defined
    Seq(1) shouldNotBe empty

    Map() shouldBe empty

    Array(1) shouldBe defined
    Array(1) shouldNotBe empty
  }

  test("support tuples") {
    (1, 2, 3) shouldBe (1, 2, 3)
    (1, 2, 3) shouldNotBe (1, 3, 2)
    (1, 'a') shouldBe (1, 'a')
    (2, 'a') shouldNotBe (2, 'b')
  }

  test("support array deepEqual") {
    val a: AnyRef = Array(1.0f)
    val b: AnyRef = Array(1.0f)

    a shouldBe a
    a shouldBe b

    a shouldBeTheSameInstanceAs a
    a shouldNotBeTheSameInstanceAs b
  }

  case class MyObj(id: Int, name: String)

  test("support equality check") {
    val a1 = MyObj(1, "name")
    val a2 = a1
    val a3 = MyObj(1, "name")

    a1 shouldBe a2
    a1 shouldBe a3
    a1 shouldBeTheSameInstanceAs a2
    a1 shouldNotBeTheSameInstanceAs a3
  }

  protected def checkEqual[A, B](a: A, b: B): Unit = {
    a shouldBe b
    intercept[AssertionFailure] {
      a shouldNotBe b
    }
  }

  protected def checkNotEqual[A, B](a: A, b: B): Unit = {
    a shouldNotBe b
    intercept[AssertionFailure] {
      a shouldBe b
    }
  }

  test("exhaustive check") {
    checkEqual(1, 1)
    checkNotEqual(1, 2)

    checkEqual("apple", "apple")
    checkNotEqual("apple", "banana")

    val list = Seq(
      (1, 2),
      ("apple", "banana"),
      (Seq(1), Seq(1, 2)),
      (Seq(Seq(1), 2, 3), Seq(Seq(2), 2, 3)),
      (List(1, 2, 3), List(1, 2)),
      (List.empty, List(1)),
      (IndexedSeq(1), IndexedSeq(2)),
      (Map(1 -> 2), Map(1 -> 3)),
      (Map(1 -> 2), Map(1 -> 'a')),
      (Map(1 -> 2), List((1, 2))),
      (Set(1, 2), Set(1, 2, 3)),
      (Vector(1, 2), Vector(1, 2, 3)),
      ((1, 'a', 1), (1, 'a', 2)),
      (Some("a"), Some("b")),
      (Some("a"), None),
      (None, Some("a")),
      (Array(1, 2), Array(1)),
      (Array(1L, 2L), Array(2L)),
      (Array(1.toShort), Array(2.toShort)),
      (Array(1.toByte), Array(2.toByte)),
      (Array('a'), Array('b')),
      (Array(1f), Array(2f)),
      (Array(1.0), Array(2.0)),
      (Array(true, false), Array(false, true)),
      (Array(1, 1L, "hello"), Array[Any]()),
      (Array(1, 2, Array(1, 2)), Array(1, 2, Array(1, 3))),
      (MyObj(1, "a"), MyObj(1, "b"))
    )

    for ((a, b) <- list) {
      checkEqual(a, a)
      checkNotEqual(a, b)

      a shouldBeTheSameInstanceAs a
      a shouldNotBeTheSameInstanceAs b
      b shouldBeTheSameInstanceAs b
    }
  }

  test("support shouldBe null") {
    val a: String = null
    a shouldBe null
    "a" shouldNotBe null

    checkEqual(a, null)
    checkNotEqual("a", null)
  }
}
