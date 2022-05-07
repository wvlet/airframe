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

import wvlet.airframe.Session
import wvlet.airframe.surface.Surface
import wvlet.airspec.spi.AirSpecContext
import wvlet.airspec.{AirSpecDef, AirSpecSpi}
import wvlet.log.LogSupport

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

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

  /**
    * A list of child tests, which are not yet started. As Scala Future starts the body evaluation eagerly, wrapping
    * Future with a no-argument function.
    */
  private val childTestList: ListBuffer[() => Future[Unit]] = ListBuffer.empty

  override def hasChildTask: Boolean = {
    synchronized {
      childTestList.size > 0
    }
  }

  override protected[airspec] def runSingle(testDef: AirSpecDef): Unit = {
    // Lazily generate Futures for child tasks
    val taskResult: () => Future[Unit] = { () =>
      taskExecutor.runSingle(Some(this), currentSession, currentSpec, testDef, isLocal = true, design = testDef.design)
    }
    synchronized {
      childTestList += taskResult
    }
  }

  private[airspec] override def childTests: Seq[() => Future[Unit]] = {
    synchronized {
      childTestList.toSeq
    }
  }
}
