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
package wvlet.airframe.examples.airspec

import wvlet.airframe._
import wvlet.airspec.AirSpec

/**
  */
class AirSpec_04_Session extends AirSpec {
  override protected def design: Design = {
    newDesign.bind[String].toInstance("hello")
  }

  def overrideDesign(session: Session, s: String): Unit = {
    s shouldBe "hello"

    val d = newDesign
      .bind[String].toInstance("hello child")

    session.withChildSession(d) { childSession =>
      val cs = childSession.build[String]
      cs shouldBe "hello child"
    }
  }
}
