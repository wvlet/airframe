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
package wvlet.airframe.legacy

import wvlet.airspec.AirSpec

import scala.util.Random
import wvlet.airframe._

object ConstructorInjectionTest {

  case class Dep1(x: Int = Random.nextInt(1000))

  case class Rep(d1: Dep1, d2: Dep1)

  case class Config(port: Int = 8080, timeoutMillis: Int = 100000)

}

/**
  */
class ConstructorInjectionTest extends AirSpec {

  import ConstructorInjectionTest._

  test("constructor injection should bind singleton to the same type") {
    newSilentDesign.build[Rep] { r =>
      r.d1 shouldBeTheSameInstanceAs r.d2
    }
  }

  test("properly populate default values") {
    newSilentDesign.build[Config] { config =>
      config shouldBe Config()
    }
  }
}
