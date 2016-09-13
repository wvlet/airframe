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

trait HelloBind {
}

trait BindExample {
  val a = bind[HelloBind]
}

/**
  *
  */
class SessionTest extends AirframeSpec {
  "Session" should {
    "pre-compile session injection template" taggedAs("session-inject") in {
      val session = newDesign.newSession
      val b = session.build[BindExample]
      b shouldBe a [BindExample]
    }

    "pre-compile singleton binding" taggedAs("singleton-inject") in {
      val session = newDesign
                    .bind[HelloBind].toEagerSingleton
                    .newSession

      val b = session.build[BindExample]
      b shouldBe a [BindExample]
    }

  }
}
