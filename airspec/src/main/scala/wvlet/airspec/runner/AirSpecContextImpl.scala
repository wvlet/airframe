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
import wvlet.airframe.surface.{MethodSurface, Surface}
import wvlet.airspec.AirSpecSpi
import wvlet.airspec.spi.AirSpecContext
import wvlet.log.LogSupport

import scala.language.experimental.macros

/**
  *
  */
private[airspec] class AirSpecContextImpl(taskExecutor: AirSpecTaskRunner,
                                          val parentContext: Option[AirSpecContext],
                                          val currentSpec: AirSpecSpi,
                                          val testName: String = "<init>",
                                          val currentSession: Session)
    extends AirSpecContext
    with LogSupport {

  override protected def runInternal(spec: AirSpecSpi, testMethods: Seq[MethodSurface]): AirSpecSpi = {
    taskExecutor.run(Some(this), spec, testMethods)
    spec
  }
  override protected def newSpec(specSurface: Surface): AirSpecSpi = {
    val spec: AirSpecSpi = currentSession.get(specSurface)
    // When the spec instance is an anonymous class, we need to find the real class name from the specSurface
    spec.setSpecName(specSurface.fullName)
    spec
  }
}
