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

import java.util.concurrent.atomic.AtomicBoolean

import javax.annotation.PreDestroy
import wvlet.airspec.AirSpec

/**
  */
class CloseableHookPrecedenceTest extends AirSpec {
  class A extends AutoCloseable {
    val closeIsCalled    = new AtomicBoolean(false)
    val shutdownIsCalled = new AtomicBoolean(false)

    override def close(): Unit = {
      closeIsCalled.set(true)
    }

    @PreDestroy
    def shutdown: Unit = {
      shutdownIsCalled.set(true)
    }
  }

  def `support closeable`: Unit = {
    val a = new A
    val d = newSilentDesign.bind[A].toInstance(a)
    d.build[A] { a => }

    a.closeIsCalled.get shouldBe false
    a.shutdownIsCalled.get() shouldBe true
  }
}
