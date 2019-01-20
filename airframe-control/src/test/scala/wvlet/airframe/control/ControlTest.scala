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
package wvlet.airframe.control

import wvlet.airframe.AirframeSpec

object ControlTest {
  class A extends AutoCloseable {
    var closed: Boolean = false
    override def close(): Unit = {
      closed = true
    }
  }
}

/**
  *
  */
class ControlTest extends AirframeSpec {
  "Control" should {
    "have loan pattern" in {
      val out = new ControlTest.A
      out.closed shouldBe false
      Control.withResource(out) { o =>
        // do nothing
      }
      out.closed shouldBe true
    }
    "have loan pattern for two resources" in {
      val in  = new ControlTest.A
      val out = new ControlTest.A
      out.closed shouldBe false
      Control.withResources(in, out) { (i, o) =>
        // do nothing
      }
      in.closed shouldBe true
      out.closed shouldBe true
    }
    "ignore resource closing errors" in {
      Control.withResource(null) { o =>
        // do nothing
      }

      Control.withResource(new AutoCloseable {
        override def close(): Unit = ???
      }) { o =>
        // do nothing
      }
    }
  }
}
