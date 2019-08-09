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

import scala.language.experimental.macros
import scala.reflect.macros.{blackbox => sm}

/**
  * A base trait to use for writing test cases
  */
trait AirSpec extends AirSpecBase with Asserts

/**
  * If no assertion support is necessary, extend this trait.
  */
trait AirSpecBase extends AirSpecSpi with PlatformAirSpec

private[spec] trait AirSpecSpi {
  protected var _methodSurfaces: Seq[MethodSurface] = compat.methodSurfacesOf(this.getClass)
  private[spec] def testMethods: Seq[MethodSurface] = {
    _methodSurfaces.filter(x => x.isPublic)
  }

  /**
    * This will add Scala.js support to the AirSpec.
    *
    * Scala.js does not support runtime reflection, so the user needs to
    * explicitly create Seq[MethodSurface] at compile-time.
    * This method is a helper method to populate methodSurfaces automatically.
    */
  protected def scalaJsSupport: Unit = macro AirSpecSpi.scalaJsSupportImpl

  protected def beforeAll(design: Design): Design = design
  protected def before(design: Design): Design    = design
  protected def after: Unit                       = {}
  protected def afterAll: Unit                    = {}
}

private[spec] object AirSpecSpi {

  /**
    * This wrapper is used for accessing protected methods in AirSpec
    */
  private[spec] implicit class AirSpecAccess(val airSpec: AirSpecSpi) extends AnyVal {
    def callBeforeAll(design: Design): Design = airSpec.beforeAll(design)
    def callBefore(design: Design): Design    = airSpec.before(design)
    def callAfter: Unit                       = airSpec.after
    def callAfterAll: Unit                    = airSpec.afterAll
  }

  def scalaJsSupportImpl(c: sm.Context): c.Tree = {
    import c.universe._
    val t = c.prefix.actualType.typeSymbol
    q"if(wvlet.airframe.spec.compat.isScalaJs) { ${c.prefix}._methodSurfaces = wvlet.airframe.surface.Surface.methodsOf[${t}] }"
  }

}
