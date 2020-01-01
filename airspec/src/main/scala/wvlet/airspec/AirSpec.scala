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
package wvlet.airspec

import wvlet.airframe.Design
import wvlet.airspec.spi.{Asserts, RichAsserts}
import wvlet.airframe.surface.{MethodSurface, Surface}
import wvlet.airspec

import scala.language.experimental.macros
import scala.reflect.macros.{blackbox => sm}

/**
  * A base trait to use for writing test cases
  */
trait AirSpec extends AirSpecBase with Asserts with RichAsserts

/**
  * If no assertion support is necessary, extend this trait.
  */
trait AirSpecBase extends AirSpecSpi with PlatformAirSpec

private[airspec] class AirSpecTestBuilder(spec: AirSpecSpi, val name: String, val design: Design)
    extends wvlet.log.LogSupport {
  def addLocalTestDef(specDef: AirSpecDef) {
    spec.addLocalTestDef(specDef)
  }
  def apply[R](body: => R): Unit = macro AirSpecMacros.test0Impl[R]
  def apply[D1, R](body: D1 => R): Unit = macro AirSpecMacros.test1Impl[D1, R]
  def apply[D1, D2, R](body: (D1, D2) => R): Unit = macro AirSpecMacros.test2Impl[D1, D2, R]
  def apply[D1, D2, D3, R](body: (D1, D2, D3) => R): Unit = macro AirSpecMacros.test3Impl[D1, D2, D3, R]
}

private[airspec] trait AirSpecSpi {
  private var _localTestDefs: List[AirSpecDef] = List.empty
  private[airspec] def addLocalTestDef(specDef: AirSpecDef) {
    synchronized {
      _localTestDefs = specDef :: _localTestDefs
    }
  }

  protected def test(name: String, design: Design = Design.empty): AirSpecTestBuilder =
    new AirSpecTestBuilder(this, name, design)

  /*
   * Design note: Ideally we should list test methods just by using the name of classes implementing AirSpecSpi without
   * instantiating test instances. However, this was impossible in Scala.js, which has only limited reflection support.
   * So we are using scalaJsSupport method.
   *
   * If we don't need to support Scala.js, we will just use RuntimeSurface to get a list of test methods.
   */
  protected var _methodSurfaces: Seq[MethodSurface] = compat.methodSurfacesOf(this.getClass)
  private[airspec] def testDefinitions: Seq[AirSpecDef] = {
    AirSpecSpi.collectTestMethods(_methodSurfaces) ++ _localTestDefs.reverse
  }

  private[airspec] var specName: String = {
    AirSpecSpi.decodeClassName(compat.getSpecName(this.getClass))
  }

  private[airspec] def setSpecName(newSpecName: String): Unit = {
    specName = newSpecName
  }

  private[airspec] def leafSpecName: String = {
    AirSpecSpi.leafClassName(specName)
  }

  /**
    * This will add Scala.js support to the AirSpec.
    *
    * Scala.js does not support runtime reflection, so the user needs to
    * explicitly create Seq[MethodSurface] at compile-time.
    * This method is a helper method to populate methodSurfaces automatically.
    */
  protected def scalaJsSupport: Unit = macro AirSpecSpi.scalaJsSupportImpl

  /**
    * Provide a global design for this spec.
    */
  protected def design: Design = Design.empty

  /**
    * Provide a test-case local design in the spec.
    *
    * Note that if you override a global design in this method,
    * test cases will create test-case local instances (singletons)
    */
  protected def localDesign: Design = Design.empty

  protected def beforeAll: Unit = {}
  protected def before: Unit    = {}
  protected def after: Unit     = {}
  protected def afterAll: Unit  = {}

  // Returns true if this is running in TravisCI
  protected def inCI: Boolean           = airspec.inCI
  protected def inTravisCI: Boolean     = airspec.inTravisCI
  protected def inCircleCI: Boolean     = airspec.inCircleCI
  protected def inGitHubAction: Boolean = airspec.inGitHubAction

  protected def isScalaJS: Boolean = compat.isScalaJs
}

private[airspec] object AirSpecSpi {
  private[airspec] def collectTestMethods(methodSurfaces: Seq[MethodSurface]): Seq[AirSpecDef] = {
    methodSurfaces.filter(m => m.isPublic).map(x => MethodAirSpecDef(x))
  }

  /**
    * This wrapper is used for accessing protected methods in AirSpec
    */
  private[airspec] implicit class AirSpecAccess(val airSpec: AirSpecSpi) extends AnyVal {
    def callDesign: Design      = airSpec.design
    def callLocalDesign: Design = airSpec.localDesign
    def callBeforeAll: Unit     = airSpec.beforeAll
    def callBefore: Unit        = airSpec.before
    def callAfter: Unit         = airSpec.after
    def callAfterAll: Unit      = airSpec.afterAll
  }

  def scalaJsSupportImpl(c: sm.Context): c.Tree = {
    import c.universe._
    val t = c.prefix.actualType.typeSymbol
    q"if(wvlet.airspec.compat.isScalaJs) { ${c.prefix}._methodSurfaces = wvlet.airframe.surface.Surface.methodsOf[${t}] }"
  }

  private[airspec] def decodeClassName(clsName: String): String = {
    // the full class name
    val decodedClassName = scala.reflect.NameTransformer.decode(clsName)

    // For object names ending with $
    if (decodedClassName.endsWith("$")) {
      decodedClassName.substring(0, decodedClassName.length - 1)
    } else {
      decodedClassName
    }
  }

  private[airspec] def leafClassName(fullClassName: String): String = {
    // the full class name
    val pos = fullClassName.lastIndexOf('.')
    val leafName = {
      if (pos == -1)
        fullClassName
      else {
        fullClassName.substring((pos + 1).min(fullClassName.length - 1))
      }
    }
    leafName.replaceAll("\\$", ".")
  }
}
