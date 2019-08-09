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
package wvlet.airframe.spec

import wvlet.airframe.Design
import wvlet.airframe.spec.spi.Asserts
import wvlet.airframe.surface.MethodSurface

/**
  * A base trait to use for writing test cases
  */
trait AirSpec extends AirSpecCore with Asserts

/**
  * If no assertion support is necessary, extend this trait.
  */
trait AirSpecCore extends AirSpecSpi with AirSpecBase

trait AirSpecSpi {
  private[spec] def methodSurfaces: Seq[MethodSurface] = compat.methodSurfacesOf(this.getClass)
  private[spec] def testMethods: Seq[MethodSurface] = {
    methodSurfaces.filter(x => x.isPublic)
  }

  protected def beforeAll(design: Design): Design = design
  protected def before(design: Design): Design    = design
  protected def after: Unit                       = {}
  protected def afterAll: Unit                    = {}
}

object AirSpecSpi {

  /**
    * This wrapper is used for accessing protected methods in AirSpec
    */
  private[spec] implicit class AirSpecAccess(val airSpec: AirSpecSpi) extends AnyVal {
    def callBeforeAll(design: Design): Design = airSpec.beforeAll(design)
    def callBefore(design: Design): Design    = airSpec.before(design)
    def callAfter: Unit                       = airSpec.after
    def callAfterAll: Unit                    = airSpec.afterAll
  }

}
