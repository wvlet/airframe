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
package wvlet.airspec.spi

import wvlet.airframe.Session
import wvlet.airspec.{AirSpecDef, AirSpecSpi}

import scala.concurrent.Future

/**
  * AirSpecContext is an interface to pass the test environment data to individual test methods.
  *
  * Spec: global
  */
trait AirSpecContext {
  def hasChildTask: Boolean

  def currentSpec: AirSpecSpi
  def parentContext: Option[AirSpecContext]

  /**
    * Get the current session used for calling the test method
    */
  def currentSession: Session

  /**
    * Get the leaf name of the current spec class.
    *
    * Note: In Scala.js, this may return weired names especially for inner classes.
    */
  def specName: String = currentSpec.leafSpecName

  def fullSpecName: String = {
    val parent = parentContext.map(x => s"${x.fullSpecName}.").getOrElse("")
    s"${parent}${specName}"
  }

  /**
    * Get the test method name currently running. For a global context, this will return '<init>'.
    */
  def testName: String

  lazy val indentLevel: Int = {
    parentContext.map(_.indentLevel + 1).getOrElse(0)
  }

  protected[airspec] def runSingle(testDef: AirSpecDef): Unit

  private[airspec] def childResults: Seq[Future[Unit]]
}
