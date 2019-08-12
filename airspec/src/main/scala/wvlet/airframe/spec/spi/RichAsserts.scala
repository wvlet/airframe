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

  // Here we do not extend AnyVal to make this enrichment available as trait

  implicit class ShouldBeIntArray(val value: Array[Int]) {
    def shouldBe(expected: Array[Int])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw AssertionFailure(s"${value} didn't match with ${expected}", code)
      }
    }

    def shouldNotBe(unexpected: Array[Int])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw AssertionFailure(s"${value} match with ${unexpected}", code)
      }
    }
  }

  implicit class ShouldBeShortArray(val value: Array[Short]) {
    def shouldBe(expected: Array[Short])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw AssertionFailure(s"${value} didn't match with ${expected}", code)
      }
    }

    def shouldNotBe(unexpected: Array[Short])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw AssertionFailure(s"${value} match with ${unexpected}", code)
      }
    }
  }

  implicit class ShouldBeByteArray(val value: Array[Byte]) {
    def shouldBe(expected: Array[Byte])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw AssertionFailure(s"${value} didn't match with ${expected}", code)
      }
    }

    def shouldNotBe(unexpected: Array[Byte])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw AssertionFailure(s"${value} match with ${unexpected}", code)
      }
    }
  }

  implicit class ShouldBeBooleanArray(val value: Array[Boolean]) {
    def shouldBe(expected: Array[Boolean])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw AssertionFailure(s"${value} didn't match with ${expected}", code)
      }
    }

    def shouldNotBe(unexpected: Array[Boolean])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw AssertionFailure(s"${value} match with ${unexpected}", code)
      }
    }
  }

  implicit class ShouldBeCharArray(val value: Array[Char]) {
    def shouldBe(expected: Array[Char])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw AssertionFailure(s"${value} didn't match with ${expected}", code)
      }
    }

    def shouldNotBe(unexpected: Array[Char])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw AssertionFailure(s"${value} match with ${unexpected}", code)
      }
    }
  }

  implicit class ShouldBeFloatArray(val value: Array[Float]) {
    def shouldBe(expected: Array[Float])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw AssertionFailure(s"${value} didn't match with ${expected}", code)
      }
    }

    def shouldNotBe(unexpected: Array[Float])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw AssertionFailure(s"${value} match with ${unexpected}", code)
      }
    }
  }

  implicit class ShouldBeDoubleArray(val value: Array[Double]) {
    def shouldBe(expected: Array[Double])(implicit code: SourceCode): Unit = {
      if (!util.Arrays.equals(value, expected)) {
        throw AssertionFailure(s"${value} didn't match with ${expected}", code)
      }
    }

    def shouldNotBe(unexpected: Array[Double])(implicit code: SourceCode): Unit = {
      if (util.Arrays.equals(value, unexpected)) {
        throw AssertionFailure(s"${value} match with ${unexpected}", code)
      }
    }
  }

  implicit class ShouldBeAnyArray[A](val value: Array[A]) {
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
      for (t <- Seq("Int", "Short", "Byte", "Boolean", "Char", "Float", "Double")) {
    println(
      s"""
           |implicit class ShouldBe${t}Array(val value: Array[${t}]) {
           |    def shouldBe(expected: Array[${t}])(implicit code: SourceCode): Unit = {
           |      if (!util.Arrays.equals(value, expected)) {
           |        throw AssertionFailure(s"$${value} didn't match with $${expected}", code)
           |      }
           |    }
           |
           |    def shouldNotBe(unexpected: Array[${t}])(implicit code: SourceCode): Unit = {
           |      if (util.Arrays.equals(value, unexpected)) {
           |        throw AssertionFailure(s"$${value} match with $${unexpected}", code)
           |      }
           |    }
           |}
           |
           |""".stripMargin
    )
  }
   */

  sealed trait OptionTarget {
    def matchWith[A](v: Option[A], code: SourceCode): Unit
    def flip: OptionTarget
  }
  protected case object DefinedTarget extends OptionTarget {
    override def matchWith[A](v: Option[A], code: SourceCode): Unit = {
      if (v.isEmpty) {
        throw AssertionFailure(s"${v} is empty", code)
      }
    }
    override def flip: OptionTarget = EmptyTarget
  }
  protected case object EmptyTarget extends OptionTarget {
    override def matchWith[A](v: Option[A], code: SourceCode): Unit = {
      if (v.isDefined) {
        throw AssertionFailure(s"${v} is not empty", code)
      }
    }
    override def flip: OptionTarget = DefinedTarget
  }

  protected def defined = DefinedTarget
  protected def empty   = EmptyTarget

  implicit class ShouldBeOption[A](val value: Option[A]) {
    def shouldBe(expected: OptionTarget)(implicit code: SourceCode): Unit = {
      expected.matchWith(value, code)
    }

    def shouldNotBe(expected: OptionTarget)(implicit code: SourceCode): Unit = {
      expected.flip.matchWith(value, code)
    }
  }

  implicit class ShouldBeCollection[A](val value: Seq[A]) {
    def shouldBe(expected: Seq[A])(implicit code: SourceCode): Unit = {
      if (value != expected) {
        throw AssertionFailure(s"${value} didn't match with ${expected}", code)
      }
    }
    def shouldNotBe(unexpected: Seq[A])(implicit code: SourceCode): Unit = {
      if (value == unexpected) {
        throw AssertionFailure(s"${value} didn't match with ${unexpected}", code)
      }
    }
  }

  implicit class ShouldBe(val value: Any) {
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
  }

}
