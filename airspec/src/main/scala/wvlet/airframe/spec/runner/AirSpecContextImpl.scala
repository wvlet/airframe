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
package spec.runner

import wvlet.airframe.spec.{AirSpecBase, AirSpecMacros, AirSpecSpi}
import wvlet.airframe.spec.runner.AirSpecTask.TaskExecutor
import wvlet.airframe.spec.spi.AirSpecContext
import wvlet.airframe.surface.MethodSurface
import wvlet.log.LogSupport

import scala.language.experimental.macros

/**
  *
  */
private[spec] class AirSpecContextImpl(taskExecutor: TaskExecutor,
                                       val specName: String,
                                       val testName: String,
                                       currentSession: Session)
    extends AirSpecContext
    with LogSupport {

  /**
    * Build an instance of type A using Airframe DI, and run the test method within A
    */
  override def run[A <: AirSpecBase]: Unit = {}
  override def runInternal(spec: AirSpecSpi, testMethods: Seq[MethodSurface]): Unit = {
    taskExecutor.run(spec, testMethods)
  }
}
