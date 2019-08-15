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
import wvlet.airframe.spec.spi.{Asserts, RichAsserts}
import wvlet.airframe.surface.MethodSurface

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

private[spec] trait AirSpecSpi {
  /*
   * Design note: Ideally we should list test methods just by using the name of classes implementing AirSpecSpi without
   * instantiating test instances. However, this was impossible in Scala.js, which has only limited reflection support.
   * So we are using scalaJsSupport method.
   *
   * If we don't need to support Scala.js, we will just use RuntimeSurface to get a list of test methods.
   */
  protected var _methodSurfaces: Seq[MethodSurface] = compat.methodSurfacesOf(this.getClass)
  private[spec] def testMethods: Seq[MethodSurface] = {
    AirSpecSpi.collectTestMethods(_methodSurfaces)
  }

  private[spec] var specName: String = {
    AirSpecSpi.decodeClassName(compat.getSpecName(this.getClass))
  }

  private[spec] def setSpecName(newSpecName: String): Unit = {
    specName = newSpecName
  }

  private[spec] def leafSpecName: String = {
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
    * Configure a global design for this spec.
    *
    */
  protected def configure(design: Design): Design = design

  /**
    * Configure a test-case local design in the spec.
    *
    * Note that if you override a global design in this method,
    * test cases will create test-case local instances (singletons)
    */
  protected def configureLocal(design: Design): Design = design

  protected def beforeAll: Unit = {}
  protected def before: Unit    = {}
  protected def after: Unit     = {}
  protected def afterAll: Unit  = {}

  // Returns true if this is running in TravisCI
  protected def inTravisCI: Boolean = AirSpecSpi.inTravisCI

  protected def isScalaJS: Boolean = compat.isScalaJs
}

private[spec] object AirSpecSpi {

  private[spec] def collectTestMethods(methodSurfaces: Seq[MethodSurface]): Seq[MethodSurface] = {
    methodSurfaces.filter(_.isPublic)
  }

  /**
    * This wrapper is used for accessing protected methods in AirSpec
    */
  private[spec] implicit class AirSpecAccess(val airSpec: AirSpecSpi) extends AnyVal {
    def callDesignAll(design: Design): Design  = airSpec.configure(design)
    def callDesignEach(design: Design): Design = airSpec.configureLocal(design)
    def callBeforeAll: Unit                    = airSpec.beforeAll
    def callBefore: Unit                       = airSpec.before
    def callAfter: Unit                        = airSpec.after
    def callAfterAll: Unit                     = airSpec.afterAll
  }

  def scalaJsSupportImpl(c: sm.Context): c.Tree = {
    import c.universe._
    val t = c.prefix.actualType.typeSymbol
    q"if(wvlet.airframe.spec.compat.isScalaJs) { ${c.prefix}._methodSurfaces = wvlet.airframe.surface.Surface.methodsOf[${t}] }"
  }

  private[spec] def inTravisCI: Boolean = {
    sys.env.get("TRAVIS").map(_.toBoolean).getOrElse(false)
  }

  private[spec] def decodeClassName(clsName: String): String = {
    // the full class name
    val decodedClassName = scala.reflect.NameTransformer.decode(clsName)

    // For object names ending with $
    if (decodedClassName.endsWith("$")) {
      decodedClassName.substring(0, decodedClassName.length - 1)
    } else {
      decodedClassName
    }
  }

  private[spec] def leafClassName(fullClassName: String): String = {
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
