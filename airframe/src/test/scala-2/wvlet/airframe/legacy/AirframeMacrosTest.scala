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
package wvlet.airframe.legacy

import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

import wvlet.airframe._

trait NonAbstractTrait extends LogSupport {
  debug("hello trait")
}

trait AbstractTrait extends LogSupport {
  def abstractMethod: Unit
}

trait ConcreteTrait extends AbstractTrait {
  override def abstractMethod: Unit = { debug("hello abstract trait") }
}

trait App1 {
  val t = bind[NonAbstractTrait]
}

trait App2 {
  val t = bind[AbstractTrait]
}

class ConcreteClass {
  val t = bind[NonAbstractTrait]
}

/**
  */
class AirframeMacrosTest extends AirSpec {
  scalaJsSupport

  def `build trait at compile time`: Unit = {
    val session = newDesign.newSession
    session.build[NonAbstractTrait]
    session.build[App1]
  }

  def `build abstract trait`: Unit = {
    val session = newDesign
      .bind[AbstractTrait].to[ConcreteTrait]
      .newSession

    val t   = session.build[AbstractTrait]
    val app = session.build[App2]
    t.abstractMethod
    app.t.abstractMethod
  }

  def `inject Session to concrete class`: Unit = {
    newDesign.newSession.build[ConcreteClass]
  }
}
