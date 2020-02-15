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

import wvlet.airspec.AirSpec

trait HelloBind {}

trait BindExample {
  val a = bind[HelloBind]
}

trait SessionBindExample {
  val s = bind[Session]
}

trait DesignBindExample {
  val a      = bind[HelloBind]
  val design = bind[Design]
}

/**
  *
  */
class SessionTest extends AirSpec {
  scalaJsSupport

  val d1 =
    newDesign
      .bind[HelloBind].toSingleton
      .noLifeCycleLogging

  def `pre-compile session injection template`: Unit = {
    val session = newDesign.newSession
    val b       = session.build[BindExample]
    classOf[BindExample].isAssignableFrom(b.getClass) shouldBe true
  }

  def `pre-compile singleton binding`: Unit = {
    // HelloBind should be instantiated without using runtime-eval
    val session = newDesign
      .bind[HelloBind].toEagerSingleton
      .newSession

    val b = session.build[BindExample]
    classOf[BindExample].isAssignableFrom(b.getClass) shouldBe true
  }

  def `find self session from binding`: Unit = {
    val session = newDesign
      .bind[HelloBind].toSingleton
      .newSession

    val e = session.build[SessionBindExample]
    e.s shouldBeTheSameInstanceAs (session)
  }

  def `should bind an equivalent design`: Unit = {
    d1.build[DesignBindExample] { e => e.design shouldBe d1.minimize }
  }
}
