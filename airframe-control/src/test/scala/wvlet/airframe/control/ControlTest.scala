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

import wvlet.airspec.AirSpec

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
class ControlTest extends AirSpec {
  def `have loan pattern`: Unit = {
    val out = new ControlTest.A
    out.closed shouldBe false
    Control.withResource(out) { o =>
      // do nothing
    }
    out.closed shouldBe true
  }
  def `have loan pattern for two resources`: Unit = {
    val in  = new ControlTest.A
    val out = new ControlTest.A
    out.closed shouldBe false
    Control.withResources(in, out) { (i, o) =>
      // do nothing
    }
    in.closed shouldBe true
    out.closed shouldBe true
  }
  def `not cause error for null resource`: Unit = {
    Control.withResource(null) { o =>
      // do nothing
    }
    Control.withResources(null, null) { (i, o) =>
      // do nothing
    }
  }
  def `report resource closing errors`: Unit = {
    class FirstException  extends RuntimeException
    class SecondException extends RuntimeException

    intercept[FirstException] {
      Control.withResource(new AutoCloseable {
        override def close(): Unit = throw new FirstException()
      }) { o =>
        // do nothing
      }
    }

    val m = intercept[MultipleExceptions] {
      Control.withResources(
        new AutoCloseable {
          override def close(): Unit = throw new FirstException()
        },
        new AutoCloseable {
          override def close(): Unit = throw new SecondException()
        }
      ) { (i, o) =>
        // do nothing
      }
    }
    m.causes.exists(_.getClass == classOf[FirstException]) shouldBe true
    m.causes.exists(_.getClass == classOf[SecondException]) shouldBe true
  }
}
