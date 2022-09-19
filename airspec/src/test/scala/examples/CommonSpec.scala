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

/**
  */
class CommonSpec extends AirSpec {
  test("hello") {
    assert(true)
  }

  test("failTest") {
    ignore()
    fail("test failure")
  }

  test("exceptionTest")[Unit] {
    ignore()
    throw new IllegalArgumentException("invalid argument")
  }

  test("ciChecker") {
    // sanity test
    inTravisCI
    inCircleCI
    inGitHubAction
    inCI
  }

  test("scala major version") {
    scalaMajorVersion match {
      case 2 =>
        isScala2 shouldBe true
        isScala3 shouldBe false
      case 3 =>
        isScala2 shouldBe false
        isScala3 shouldBe true
      case other =>
        throw new IllegalStateException(s"Unknown scala major version: ${other}")
    }
  }
}

object ObjSpec extends AirSpec {
  test("hello1") {
    assert(true)
  }
}
