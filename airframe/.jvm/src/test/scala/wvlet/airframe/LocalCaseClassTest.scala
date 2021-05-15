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
package wvlet.airframe

import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

/**
  * Reproduction of https://github.com/wvlet/airframe/pull/423
  * Locally created case classes should be visible inside the generated code.
  */
class LocalCaseClassTest extends AirSpec {
  case class LocalConfigA()
  case class LocalConfigB(param1: Int = 5, param2: Int = 4)

  test("should not cause any compilation error") {
    Surface.of[LocalConfigA]
    Surface.of[LocalConfigB]
    val d = newDesign
      .bind[LocalConfigA].toInstance(LocalConfigA())
      .bind[LocalConfigB].toInstance(LocalConfigB(param2 = 1))
      .noLifeCycleLogging

    d.build[LocalConfigB] { b =>
      b.param1 shouldBe 5
      b.param2 shouldBe 1
    }
  }
}
