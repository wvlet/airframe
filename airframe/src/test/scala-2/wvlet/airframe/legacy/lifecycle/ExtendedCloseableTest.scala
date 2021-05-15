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
package wvlet.airframe.lifecycle

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import wvlet.airframe.Design
import wvlet.airspec.AirSpec

/**
  */
class ExtendedCloseableTest extends AirSpec {
  scalaJsSupport

  private val closeCount = new AtomicInteger(0)

  trait A extends AutoCloseable {
    override def close(): Unit = {
      closeCount.incrementAndGet()
    }
  }

  trait B extends A

  import wvlet.airframe._

  test("close only once") {
    val d = newSilentDesign
      .bind[A].to[B]

    closeCount.get() shouldBe 0
    d.build[A] { a =>
      //
    }
    closeCount.get() shouldBe 1
  }
}
