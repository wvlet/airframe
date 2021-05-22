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
package wvlet.airframe.di

import wvlet.airframe.{Design, Session}
import wvlet.airspec.AirSpec

class HelloBind {}

class BindExample(val a: HelloBind)
class SessionBindExample(val s: Session)
class DesignBindExample(val a: HelloBind, val design: Design)

/**
  */
class SessionTest extends AirSpec {
  val d1 =
    Design.newDesign
      .bind[HelloBind].toSingleton
      .noLifeCycleLogging

  test("pre-compile session injection template") {
    val session = Design.newDesign.newSession
    val b       = session.build[BindExample]
    classOf[BindExample].isAssignableFrom(b.getClass) shouldBe true
  }

  test("pre-compile singleton binding") {
    // HelloBind should be instantiated without using runtime-eval
    val session = Design.newDesign
      .bind[HelloBind].toEagerSingleton
      .newSession

    val b = session.build[BindExample]
    classOf[BindExample].isAssignableFrom(b.getClass) shouldBe true
  }

  test("find self session from binding") {
    val session = Design.newDesign
      .bind[HelloBind].toSingleton
      .newSession

    val e = session.build[SessionBindExample]
    e.s shouldBeTheSameInstanceAs (session)
  }

  test("should bind an equivalent design") {
    d1.build[DesignBindExample] { e => e.design shouldBe d1.minimize }
  }
}
