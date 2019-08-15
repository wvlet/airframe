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
package wvlet.airframe.spec.spi

import wvlet.airframe.spec.{AirSpecBase, AirSpecMacros, AirSpecSpi}
import wvlet.airframe.surface.{MethodSurface, Surface}

import scala.language.experimental.macros

/**
  *
  */
trait AirSpecContext {
  def specName: String
  def testName: String

  /**
    * Build an instance of type A using Airframe DI, and run the test method within A.
    * @return the generated instance of A
    */
  def run[A <: AirSpecBase]: A = macro AirSpecMacros.runImpl[A]

  /**
    * Run the test methods in a given AirSpec instance
    */
  def run[A <: AirSpecBase](spec: A): A = macro AirSpecMacros.runSpecImpl[A]

  protected def runInternal(spec: AirSpecSpi, testMethods: Seq[MethodSurface]): AirSpecSpi
  protected def newSpec(specSurface: Surface): AirSpecSpi
}

object AirSpecContext {

  implicit class AirSpecContextAccess(val context: AirSpecContext) extends AnyVal {
    def callRunInternal(spec: AirSpecSpi, testMethods: Seq[MethodSurface]): AirSpecSpi = {
      context.runInternal(spec, testMethods)
    }
    def callNewSpec(specSurface: Surface): AirSpecSpi = context.newSpec(specSurface)
  }
}
