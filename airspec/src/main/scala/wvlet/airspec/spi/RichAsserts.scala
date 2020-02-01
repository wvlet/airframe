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
package wvlet.airspec.spi

import java.util

import wvlet.airframe.SourceCode
import wvlet.airspec.AirSpecSpi
import wvlet.log.LogSupport

/**
  *
  */
trait RichAsserts extends LogSupport { this: AirSpecSpi =>

  // Here we do not extend implicit classes with AnyVal, which needs to be a public class in an object,
  // to make this enrichment available as trait

  private def pp(v: Any): String = {
    v match {
      case null =>
        "null"
      case a: Array[_] =>
        s"[${a.mkString(",")}]"
      case _ =>
        v.toString
    }
  }

  sealed trait TestResult
  case object Ok     extends TestResult
  case object Failed extends TestResult

  private def check(cond: Boolean): TestResult = {
    if (cond) {
      Ok
    } else {
      Failed
    }
  }

  private[airspec] sealed trait OptionTarget {
    def check[A](v: A, isEmpty: Boolean, code: SourceCode): Unit
    def flip: OptionTarget
  }

  private[airspec] case object DefinedTarget extends OptionTarget {
    override def check[A](v: A, isEmpty: Boolean, code: SourceCode): Unit = {
      if (isEmpty) {
        throw AssertionFailure(s"${v} is empty", code)
      }
    }
    override def flip: OptionTarget = EmptyTarget
  }

  private[airspec] case object EmptyTarget extends OptionTarget {
    override def check[A](v: A, isEmpty: Boolean, code: SourceCode): Unit = {
      if (!isEmpty) {
        throw AssertionFailure(s"${v} is not empty", code)
      }
    }
    override def flip: OptionTarget = DefinedTarget
  }

  protected def defined: OptionTarget = DefinedTarget
  protected def empty: OptionTarget   = EmptyTarget

  private def arrayDeepEqualMatcher: PartialFunction[(Any, Any), TestResult] = {
    case (a: Array[Int], b: Array[Int])         => check(util.Arrays.equals(a, b))
    case (a: Array[Short], b: Array[Short])     => check(util.Arrays.equals(a, b))
    case (a: Array[Byte], b: Array[Byte])       => check(util.Arrays.equals(a, b))
    case (a: Array[Char], b: Array[Char])       => check(util.Arrays.equals(a, b))
    case (a: Array[Long], b: Array[Long])       => check(util.Arrays.equals(a, b))
    case (a: Array[Boolean], b: Array[Boolean]) => check(util.Arrays.equals(a, b))
    case (a: Array[Float], b: Array[Float])     => check(util.Arrays.equals(a, b))
    case (a: Array[Double], b: Array[Double])   => check(util.Arrays.equals(a, b))
    case (a: Array[AnyRef], b: Array[AnyRef]) =>
      check(
        util.Arrays
          .deepEquals(a.asInstanceOf[Array[java.lang.Object]], b.asInstanceOf[Array[java.lang.Object]])
      )
    case (a: Iterable[_], b: Iterable[_]) => check(a == b)
    case (a: Product, b: Product)         => check(a == b)
  }

  implicit protected class ShouldBe(val value: Any) {
    protected def matchFailure(expected: Any, code: SourceCode): AssertionFailure = {
      AssertionFailure(s"${pp(value)} didn't match with ${pp(expected)}", code)
    }
    protected def unmatchFailure(unexpected: Any, code: SourceCode): AssertionFailure = {
      AssertionFailure(s"${pp(value)} matched with ${pp(unexpected)}", code)
    }

    private def test(expected: Any): TestResult = {
      arrayDeepEqualMatcher
        .orElse[(Any, Any), TestResult] {
          case _ =>
            check(value == expected)
        }
        .apply(value, expected)
    }

    def shouldBe(expected: Any)(implicit code: SourceCode): Boolean = {
      test(expected) match {
        case Ok => true
        case Failed =>
          throw matchFailure(expected, code)
      }
    }

    def shouldNotBe(unexpected: Any)(implicit code: SourceCode): Boolean = {
      test(unexpected) match {
        case Ok =>
          throw unmatchFailure(unexpected, code)
        case Failed =>
          true
      }
    }

    def shouldBe(expected: OptionTarget)(implicit code: SourceCode) = {
      if (expected == null) {
        if (value != null) {
          throw AssertionFailure(s"${pp(value)} should be null", code)
        }
      } else {
        value match {
          case v: String =>
            expected.check(value, v.isEmpty, code)
          case v: Option[_] =>
            expected.check(value, v.isEmpty, code)
          case v: Iterable[_] =>
            expected.check(value, v.isEmpty, code)
          case v: Array[_] =>
            expected.check(value, v.isEmpty, code)
          case _ =>
            throw AssertionFailure(s"${pp(value)} is not an Option", code)
        }
      }
    }

    def shouldNotBe(expected: OptionTarget)(implicit code: SourceCode) = {
      if (expected == null) {
        if (value == null) {
          throw AssertionFailure(s"${pp(value)} should not be null", code)
        }
      } else {
        value match {
          case v: String =>
            expected.flip.check(value, v.isEmpty, code)
          case v: Option[_] =>
            expected.flip.check(value, v.isEmpty, code)
          case v: Iterable[_] =>
            expected.flip.check(value, v.isEmpty, code)
          case v: Array[_] =>
            expected.flip.check(value, v.isEmpty, code)
          case _ =>
            throw AssertionFailure(s"${pp(value)} is not an Option", code)
        }
      }
    }

    def shouldBeTheSameInstanceAs(expected: Any)(implicit code: SourceCode): Unit = {
      (value, expected) match {
        case (a: AnyRef, b: AnyRef) if a eq b =>
        // OK
        case _ =>
          throw AssertionFailure(s"${pp(value)} is not the same instance with ${pp(expected)}", code)
      }
    }

    def shouldNotBeTheSameInstanceAs(expected: Any)(implicit code: SourceCode): Unit = {
      (value, expected) match {
        case (a: AnyRef, b: AnyRef) if a ne b =>
        // OK
        case _ =>
          throw AssertionFailure(s"${pp(value)} should not be the same instance as ${pp(expected)}", code)
      }
    }
  }
}
