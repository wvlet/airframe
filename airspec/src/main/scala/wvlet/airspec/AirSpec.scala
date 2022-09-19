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
import wvlet.airframe.surface.{MethodSurface, Surface}
import wvlet.airspec
import wvlet.airspec.spi.{AirSpecContext, Asserts, RichAsserts}

/**
  * A base trait to use for writing test cases
  */
trait AirSpec extends AirSpecBase with Asserts with RichAsserts

/**
  * If no assertion support is necessary, extend this trait.
  */
trait AirSpecBase extends AirSpecSpi with PlatformAirSpec

private[airspec] trait AirSpecSpi extends AirSpecSpiCompat {
  private[airspec] var _currentContext: List[AirSpecContext] = List.empty
  private[airspec] def pushContext(ctx: AirSpecContext): Unit = {
    synchronized {
      _currentContext = ctx :: _currentContext
    }
  }
  private[airspec] def popContext: Unit = {
    synchronized {
      if (_currentContext.nonEmpty) {
        _currentContext = _currentContext.tail
      }
    }
  }

  private var _localTestDefs: List[AirSpecDef] = List.empty
  private[airspec] def addLocalTestDef(specDef: AirSpecDef): Unit = {
    synchronized {
      _currentContext match {
        case Nil =>
          _localTestDefs = specDef :: _localTestDefs
        case ctx :: _ =>
          ctx.runSingle(specDef)
      }
    }
  }

  /**
    * Register a new test. If a custom Design is provided, it will be used to populate the arguments of the test body
    * method.
    */
  protected def test(name: String, design: Design = Design.empty): AirSpecTestBuilder = {
    new AirSpecTestBuilder(this, name, design)
  }

  private[airspec] def testDefinitions: Seq[AirSpecDef] = {
    _localTestDefs.reverse
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
    * Provide a global design for this spec.
    */
  protected def design: Design = Design.empty

  /**
    * Provide a test-case local design in the spec.
    *
    * Note that if you override a global design in this method, test cases will create test-case local instances
    * (singletons)
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

  protected def isScala2: Boolean = scalaMajorVersion == 2
  protected def isScala3: Boolean = scalaMajorVersion == 3

  /**
    * Provide a platform-independent execution context for async testing
    */
  protected def defaultExecutionContext: scala.concurrent.ExecutionContext = compat.executionContext
}

private[airspec] object AirSpecSpi {

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
