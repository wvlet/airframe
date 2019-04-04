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
import wvlet.airframe.tracing.ChromeTracer
import wvlet.log.LogSupport

object TracerTest {

  trait D
  trait E

  trait C {
    val d = bind[D]
    val e = bind[E]
  }

  trait B {
    val c = bind[C]
  }

  trait A extends LogSupport {
    val b = bind[B]
      .onInject(x => info("inject"))
      .onInit(x => info("init"))
      .onStart(x => info("start"))
      .beforeShutdown(x => info("befoer shutdown"))
      .onShutdown(x => info("shutdown"))
  }

}

/**
  *
  */
class TracerTest extends AirframeSpec {

  import TracerTest._

  "trace events" in {

    val d = newDesign
      .withTracer(ChromeTracer.newTracer("target/trace.json"))

    d.build[A] { a =>
      //
    }

  }

}
