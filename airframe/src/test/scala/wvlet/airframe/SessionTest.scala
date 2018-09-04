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
class SessionTest extends AirframeSpec {

  val d1 =
    newDesign
      .bind[HelloBind].toSingleton
      .withoutLifeCycleLogging

  "Session" should {
    "pre-compile session injection template" taggedAs ("session-inject") in {
      val session = newDesign.newSession
      val b       = session.build[BindExample]
      b shouldBe a[BindExample]
    }

    "pre-compile singleton binding" taggedAs ("singleton-inject") in {
      // HelloBind should be instantiated without using runtime-eval
      val session = newDesign
        .bind[HelloBind].toEagerSingleton
        .newSession

      val b = session.build[BindExample]
      b shouldBe a[BindExample]
    }

    "find self session from binding" in {
      val session = newDesign
        .bind[HelloBind].toSingleton
        .newSession

      val e = session.build[SessionBindExample]
      e.s shouldBe theSameInstanceAs(session)
    }

    "should contain the reference to the design" in {
      d1.build[DesignBindExample] { e =>
        e.design shouldBe theSameInstanceAs(d1)
      }
    }
  }
}
