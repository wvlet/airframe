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

import java.util.concurrent.atomic.AtomicInteger

import wvlet.airframe.newSilentDesign
import wvlet.airspec.AirSpec

/**
  */
class DesignTimeLifeCycleHookTest extends AirSpec {
  def `support design time bindings`: Unit = {
    val order              = new AtomicInteger(1)
    val initializedTime    = new AtomicInteger(0)
    val injectTime         = new AtomicInteger(0)
    val startTime          = new AtomicInteger(0)
    val beforeShutdownTime = new AtomicInteger(0)
    val shutdownTime       = new AtomicInteger(0)

    val d = newSilentDesign
      .bind[String].toInstance("hello")
      .onInit(x => initializedTime.set(order.getAndIncrement()))
      .onInject(x => injectTime.set(order.getAndIncrement()))
      .onStart(x => startTime.set(order.getAndIncrement()))
      .beforeShutdown(x => beforeShutdownTime.set(order.getAndIncrement()))
      .onShutdown(x => shutdownTime.set(order.getAndIncrement()))

    d.build[String] { s =>
      //
    }

    initializedTime.get shouldBe 1
    injectTime.get shouldBe 2
    startTime.get shouldBe 3
    beforeShutdownTime.get shouldBe 4
    shutdownTime.get shouldBe 5
  }

  def `add lifecycle only`: Unit = {
    val v = new AtomicInteger(0)
    val d = newSilentDesign
      .bind[AtomicInteger].toInstance(v)

    val d2 = d
      .bind[AtomicInteger]
      .onStart { x => x.addAndGet(1) }
      .onShutdown { x => x.addAndGet(1 << 1) }
      .beforeShutdown { x => x.addAndGet(1 << 2) }
      .onInit { x => x.addAndGet(1 << 3) }
      .onInject { x => x.addAndGet(1 << 4) }

    d2.withSession { s => }

    v.get() shouldBe 0x1f
  }

}
