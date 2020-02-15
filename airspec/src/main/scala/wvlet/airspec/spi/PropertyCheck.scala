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

import org.scalacheck.Test.{Parameters, PropException, Result}
import org.scalacheck.util.Pretty
import org.scalacheck.{Arbitrary, Gen, Prop, Shrink, Test}
import wvlet.airspec.AirSpecSpi

/**
  *
  */
trait PropertyCheck extends Asserts { this: AirSpecSpi =>

  protected def scalaCheckConfig: Parameters = Test.Parameters.default

  private def checkProperty(prop: Prop): Unit = {
    val result = Test.check(scalaCheckConfig, prop)
    if (!result.passed) {
      result match {
        case Result(PropException(args, e: AirSpecFailureBase, labels), succeeded, discarded, _, time) =>
          val reason = s"${e.message}\n${Pretty.prettyArgs(args)(Pretty.defaultParams)}"
          fail(reason)(e.code)
        case _ =>
          fail(Pretty.pretty(result))
      }
    }
  }

  private def OK: Any => Boolean = { x: Any => true }

  private def booleanProp = { x: Boolean => Prop(x) }

  protected def forAll[A1, U](checker: A1 => U)(
      implicit
      a1: Arbitrary[A1],
      s1: Shrink[A1],
      pp1: A1 => Pretty
  ): Unit = {
    val prop = Prop.forAll(checker.andThen(OK))(booleanProp, a1, s1, pp1)
    checkProperty(prop)
  }

  protected def forAll[A1, U](gen: Gen[A1])(checker: A1 => U)(implicit s1: Shrink[A1], pp1: A1 => Pretty): Unit = {
    val prop = Prop.forAll(gen)(checker.andThen(OK))(booleanProp, s1, pp1)
    checkProperty(prop)
  }

  protected def forAll[A1, A2, U](checker: (A1, A2) => U)(
      implicit
      a1: Arbitrary[A1],
      s1: Shrink[A1],
      pp1: A1 => Pretty,
      a2: Arbitrary[A2],
      s2: Shrink[A2],
      pp2: A2 => Pretty
  ): Unit = {
    val prop = Prop.forAll { (a1: A1, a2: A2) =>
      checker(a1, a2)
      true
    }(booleanProp, a1, s1, pp1, a2, s2, pp2)
    checkProperty(prop)
  }

  protected def forAll[A1, A2, U](g1: Gen[A1], g2: Gen[A2])(checker: (A1, A2) => U)(
      implicit
      s1: Shrink[A1],
      pp1: A1 => Pretty,
      s2: Shrink[A2],
      pp2: A2 => Pretty
  ): Unit = {
    val prop = Prop.forAll(g1, g2) { (a1: A1, a2: A2) =>
      checker(a1, a2)
      true
    }(booleanProp, s1, pp1, s2, pp2)
    checkProperty(prop)
  }

  protected def forAll[A1, A2, A3, U](checker: (A1, A2, A3) => U)(
      implicit
      a1: Arbitrary[A1],
      s1: Shrink[A1],
      pp1: A1 => Pretty,
      a2: Arbitrary[A2],
      s2: Shrink[A2],
      pp2: A2 => Pretty,
      a3: Arbitrary[A3],
      s3: Shrink[A3],
      pp3: A3 => Pretty
  ): Unit = {
    val prop = Prop.forAll { (a1: A1, a2: A2, a3: A3) =>
      checker(a1, a2, a3)
      true
    }(booleanProp, a1, s1, pp1, a2, s2, pp2, a3, s3, pp3)
    checkProperty(prop)
  }
}
