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
import wvlet.airframe.tracing.{ChromeTracer, DIStats}
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

object TracerTest extends LogSupport {
  class D
  class E
  class F
  class G

  class C(d: D, e: E, g: G)
  class B(c: C, e: E, g: G)
  class A(b: B)
}

/**
  */
class TracerTest extends AirSpec {
  import TracerTest.*

  test("should trace events") {
    val d = newDesign.noLifeCycleLogging
      .withTracer(ChromeTracer.newTracer("target/trace.json"))

    d.build[A] { a =>
      //
    }
  }

  test("should report design coverage") {
    val stats = new DIStats()
    val d = newDesign.noLifeCycleLogging
      .bind[A].toSingleton
      .bind[B].toSingleton
      .onInject(x => debug("inject"))
      .onInit(x => debug("init"))
      .onStart(x => debug("start"))
      .beforeShutdown(x => debug("befoer shutdown"))
      .onShutdown(x => debug("shutdown"))
      .bind[E].toSingleton
      .bind[F].toSingleton
      .bind[G].toLazyInstance(new G {})
      .noStats          // just for test coverage
      .withStats(stats) // Set stats

    d.build[A] { a =>
      //
    }

    val report = stats.coverageReportFor(d)
    val r      = report.toString
    debug(r)
  }
}
