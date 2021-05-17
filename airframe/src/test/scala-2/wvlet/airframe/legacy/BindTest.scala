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

import wvlet.airframe._

object BindTest {
  class X {
    def close(): Unit = {}
  }

  trait Bind {
    val x = bind[X] { new X }.onShutdown { _.close() }
  }
}

/**
  */
class BindTest extends AirSpec {
  import BindTest._

  test("allow provider based initialization") {
    val s = newSilentDesign.build[Bind] { b =>
      //
    }
  }
}
