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

import wvlet.airspec.AirSpec
import wvlet.airspec.spi.AssertionFailure

class ShouldContainTest extends AirSpec {
  test("shouldContain strings") {
    "hello world" shouldContain "hello"
    "hello world" shouldContain "world"
  }

  test("shouldContain Seq[X]") {
    Seq(1, 2, 3) shouldContain 1
    Seq(1, 2, 3) shouldContain 2
    Seq(1, 2, 3) shouldContain 3
  }

  test("shouldContain Array[X]") {
    Array(1, 2, 3) shouldContain 1
    Array(1, 2, 3) shouldContain 2
    Array(1, 2, 3) shouldContain 3
  }

  test("shouldContain Iterable[X]") {
    Set(1, 2, 3) shouldContain 1
    Set(1, 2, 3) shouldContain 2
    Set(1, 2, 3) shouldContain 3
  }

  test("shouldContain failure strings") {
    intercept[AssertionFailure] {
      "hello world" shouldContain "goodbye"
    }
  }

  test("shouldContain failure Seq[X]") {
    intercept[AssertionFailure] {
      Seq(1, 2, 3) shouldContain 4
    }
  }

  test("shouldContain failure Iterable[X]") {
    intercept[AssertionFailure] {
      Set(1, 2, 3) shouldContain 4
    }
  }

  test("shouldContain failure Array[X]") {
    intercept[AssertionFailure] {
      Array(1, 2, 3) shouldContain 4
    }
  }

  test("shouldNotContain strings") {
    "hello world" shouldNotContain "goodbye"
  }

  test("shouldNotContain Seq[X]") {
    Seq(1, 2, 3) shouldNotContain 4
  }

  test("shouldNotContain Array[X]") {
    Array(1, 2, 3) shouldNotContain 4
  }

  test("shouldNotContain Iterable[X]") {
    Set(1, 2, 3) shouldNotContain 4
  }

  test("shouldNotContain failure string") {
    intercept[AssertionFailure] {
      "hello world" shouldNotContain "hello"
    }
  }

  test("shouldNotContain failure Seq[X]") {
    intercept[AssertionFailure] {
      Seq(1, 2, 3) shouldNotContain 1
    }
  }

  test("shouldNotContain failure Array[X]") {
    intercept[AssertionFailure] {
      Array(1, 2, 3) shouldNotContain 1
    }
  }

  test("shouldNotContain failure Iterable[X]") {
    intercept[AssertionFailure] {
      Set(1, 2, 3) shouldNotContain 1
    }
  }

  test("shouldContain failures for unsupported inputs") {
    intercept[AssertionFailure] {
      1 shouldContain 1
    }
  }

  test("shouldNotContain failures  for unsupported inputs") {
    intercept[AssertionFailure] {
      1 shouldNotContain 1
    }
  }
}
