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
package wvlet.airspec.runner

import java.util.concurrent.atomic.AtomicInteger

import wvlet.airframe.Session
import wvlet.airframe.surface.Surface
import wvlet.airspec.spi.AirSpecContext
import wvlet.airspec.{AirSpecDef, AirSpecSpi}
import wvlet.log.LogSupport

/**
  */
private[airspec] class AirSpecContextImpl(
    taskExecutor: AirSpecTaskRunner,
    val parentContext: Option[AirSpecContext],
    val currentSpec: AirSpecSpi,
    val testName: String = "<init>",
    val currentSession: Session
) extends AirSpecContext
    with LogSupport {
  private val childTaskCount = new AtomicInteger(0)

  override def hasChildTask: Boolean = {
    childTaskCount.get > 0
  }

  override protected[airspec] def runSingle(testDef: AirSpecDef): Unit = {
    childTaskCount.incrementAndGet()
    taskExecutor.runSingle(Some(this), currentSession, currentSpec, testDef, isLocal = true, design = testDef.design)
  }
}
