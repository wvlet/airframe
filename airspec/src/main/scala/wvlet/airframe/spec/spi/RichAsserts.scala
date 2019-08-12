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
package wvlet.airframe.spec.spi

import java.util
import java.util.Comparator

import wvlet.airframe.SourceCode
import wvlet.log.LogSupport

/**
  *
  */
trait RichAsserts extends LogSupport {

  // Here we do not extend AnyVal (which needs to be a public class in an object) to make this enrichment available as trait

  protected abstract class ShouldBeArrayBase[A <: Array[_]] {
    def value: Array[_]

    protected def pp(v: Array[_]): String = {
      s"[${value.mkString(",")}]"
    }

    protected def matchFailure(expected: A, code: SourceCode): AssertionFailure = {
      AssertionFailure(s"${pp(value)} didn't match with ${pp(expected)}", code)
    }
    protected def unmatchFailure(unexpected: A, code: SourceCode): AssertionFailure = {
      AssertionFailure(s"${pp(value)} matches with ${pp(unexpected)}", code)
    }

    def shouldBe(expected: OptionTarget)(implicit code: SourceCode) = {
      expected.check(value, value.isEmpty, code)
    }

    def shouldNotBe(expected: OptionTarget)(implicit code: SourceCode) = {
      expected.flip.check(value, value.isEmpty, code)
    }
  }

  implicit protected class ShouldBeIntArray(override val value: Array[Int]) extends ShouldBeArrayBase[Array[Int]] {
    def shouldBe(expected: Array[Int])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw matchFailure(expected, code)
      }
    }
    def shouldNotBe(unexpected: Array[Int])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw unmatchFailure(unexpected, code)
      }
    }
  }

  implicit protected class ShouldBeLongArray(override val value: Array[Long]) extends ShouldBeArrayBase[Array[Long]] {
    def shouldBe(expected: Array[Long])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw matchFailure(expected, code)
      }
    }
    def shouldNotBe(unexpected: Array[Long])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw unmatchFailure(unexpected, code)
      }
    }
  }

  implicit protected class ShouldBeShortArray(override val value: Array[Short])
      extends ShouldBeArrayBase[Array[Short]] {
    def shouldBe(expected: Array[Short])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw matchFailure(expected, code)
      }
    }
    def shouldNotBe(unexpected: Array[Short])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw unmatchFailure(unexpected, code)
      }
    }
  }

  implicit protected class ShouldBeByteArray(override val value: Array[Byte]) extends ShouldBeArrayBase[Array[Byte]] {
    def shouldBe(expected: Array[Byte])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw matchFailure(expected, code)
      }
    }
    def shouldNotBe(unexpected: Array[Byte])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw unmatchFailure(unexpected, code)
      }
    }
  }

  implicit protected class ShouldBeBooleanArray(override val value: Array[Boolean])
      extends ShouldBeArrayBase[Array[Boolean]] {
    def shouldBe(expected: Array[Boolean])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw matchFailure(expected, code)
      }
    }
    def shouldNotBe(unexpected: Array[Boolean])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw unmatchFailure(unexpected, code)
      }
    }
  }

  implicit protected class ShouldBeCharArray(override val value: Array[Char]) extends ShouldBeArrayBase[Array[Char]] {
    def shouldBe(expected: Array[Char])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw matchFailure(expected, code)
      }
    }
    def shouldNotBe(unexpected: Array[Char])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw unmatchFailure(unexpected, code)
      }
    }
  }

  implicit protected class ShouldBeFloatArray(override val value: Array[Float])
      extends ShouldBeArrayBase[Array[Float]] {
    def shouldBe(expected: Array[Float])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw matchFailure(expected, code)
      }
    }
    def shouldNotBe(unexpected: Array[Float])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw unmatchFailure(unexpected, code)
      }
    }
  }

  implicit protected class ShouldBeDoubleArray(override val value: Array[Double])
      extends ShouldBeArrayBase[Array[Double]] {
    def shouldBe(expected: Array[Double])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw matchFailure(expected, code)
      }
    }
    def shouldNotBe(unexpected: Array[Double])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw unmatchFailure(unexpected, code)
      }
    }
  }

  implicit protected class ShouldBeAnyArray[A](val value: Array[A]) extends ShouldBeArrayBase[Array[A]] {
    def shouldBe(expected: Array[A])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.deepEquals(value.asInstanceOf[Array[java.lang.Object]],
                                  expected.asInstanceOf[Array[java.lang.Object]])) {
        throw AssertionFailure(s"${value} didn't match with ${expected}", code)
      }
    }

    def shouldNotBe(unexpected: Array[A])(implicit code: SourceCode): Unit = {
      if (util.Arrays.deepEquals(value.asInstanceOf[Array[java.lang.Object]],
                                 unexpected.asInstanceOf[Array[java.lang.Object]])) {
        throw AssertionFailure(s"${value} match with ${unexpected}", code)
      }
    }
  }

  // Code generator
  /*
  for (t <- Seq("Int", "Long", "Short", "Byte", "Boolean", "Char", "Float", "Double")) {
    println(
      s"""
  implicit protected class ShouldBe${t}Array(override val value: Array[${t}]) extends ShouldBeArrayBase[Array[${t}]] {
    def shouldBe(expected: Array[${t}])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw matchFailure(expected, code)
      }
    }
    def shouldNotBe(unexpected: Array[${t}])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw unmatchFailure(unexpected, code)
      }
    }
  }
"""
    )
  }
   */

  private[spec] sealed trait OptionTarget {
    def check[A](v: A, isEmpty: Boolean, code: SourceCode): Unit
    def flip: OptionTarget
  }

  private[spec] case object DefinedTarget extends OptionTarget {
    override def check[A](v: A, isEmpty: Boolean, code: SourceCode): Unit = {
      if (isEmpty) {
        throw AssertionFailure(s"${v} is empty", code)
      }
    }
    override def flip: OptionTarget = EmptyTarget
  }

  private[spec] case object EmptyTarget extends OptionTarget {
    override def check[A](v: A, isEmpty: Boolean, code: SourceCode): Unit = {
      if (!isEmpty) {
        throw AssertionFailure(s"${v} is not empty", code)
      }
    }
    override def flip: OptionTarget = DefinedTarget
  }

  protected def defined = DefinedTarget
  protected def empty   = EmptyTarget

  implicit protected class ShouldBeOption[A](val value: Option[A]) {
    def shouldBe(expected: OptionTarget)(implicit code: SourceCode): Unit = {
      expected.check(value, value.isEmpty, code)
    }

    def shouldNotBe(expected: OptionTarget)(implicit code: SourceCode): Unit = {
      expected.flip.check(value, value.isEmpty, code)
    }
  }

  implicit protected class ShouldBeIterable[A](val value: Iterable[A]) {
    def shouldBe(expected: Iterable[A])(implicit code: SourceCode): Unit = {
      if (value != expected) {
        throw AssertionFailure(s"${value} didn't match with ${expected}", code)
      }
    }

    def shouldNotBe(unexpected: Iterable[A])(implicit code: SourceCode): Unit = {
      if (value == unexpected) {
        throw AssertionFailure(s"${value} didn't match with ${unexpected}", code)
      }
    }

    def shouldBe(expected: OptionTarget)(implicit code: SourceCode) = {
      expected.check(value, value.isEmpty, code)
    }

    def shouldNotBe(expected: OptionTarget)(implicit code: SourceCode) = {
      expected.flip.check(value, value.isEmpty, code)
    }
  }

  implicit protected class ShouldBeProduct(val value: Product) {
    def shouldBe(expected: Product)(implicit code: SourceCode): Unit = {
      if (value != expected) {
        throw AssertionFailure(s"${value} didn't match with ${expected}", code)
      }
    }

    def shouldNotBe(unexpected: Product)(implicit code: SourceCode): Unit = {
      if (value == unexpected) {
        throw AssertionFailure(s"${value} didn't match with ${unexpected}", code)
      }
    }
  }

  implicit protected class ShouldBe(val value: Any) {
    def shouldBe(expected: Any)(implicit code: SourceCode): Unit = {
      if (value != expected) {
        throw AssertionFailure(s"${value} didn't match with ${expected}", code)
      }
    }

    def shouldNotBe(expected: Any)(implicit code: SourceCode): Unit = {
      if (value == expected) {
        throw AssertionFailure(s"${value} didn't match with ${expected}", code)
      }
    }

    def shouldBeTheSameInstanceAs(expected: Any)(implicit code: SourceCode): Unit = {
      (value, expected) match {
        case (a: AnyRef, b: AnyRef) if a eq b =>
        // OK
        case _ =>
          throw AssertionFailure(s"${value} is not the same instance with ${expected}", code)
      }
    }

    def shouldNotBeTheSameInstanceAs(expected: Any)(implicit code: SourceCode): Unit = {
      (value, expected) match {
        case (a: AnyRef, b: AnyRef) if a ne b =>
        // OK
        case _ =>
          throw AssertionFailure(s"${value} shoul not be the same instance as ${expected}", code)
      }
    }
  }
}
