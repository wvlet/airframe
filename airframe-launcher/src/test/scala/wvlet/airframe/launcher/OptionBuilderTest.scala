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
package wvlet.airframe.launcher

import wvlet.airspec.AirSpec

object OptionBuilderTest {
  case class Opt1(
      @option(prefix = "-e")
      env: String = "default"
  )

  case class Main1(opt: Opt1) {}
}

/**
  */
class OptionBuilderTest extends AirSpec {
  import OptionBuilderTest._

  test("read default value") {
    val l   = Launcher.of[Main1]
    val m   = l.execute("")
    val opt = m.getRootInstance.asInstanceOf[Main1].opt
    opt.env shouldBe "default"
  }
}
