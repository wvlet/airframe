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

/**
  */
class AssistedInjectionTest extends AirSpec {
  import AssistedInjectionTest._

  test("support assisted injection") {
    newSilentDesign
      .bind[MyService].toInstance("hello")
      .withSession { session =>
        val p  = session.build[NamedServiceProvider]
        val a1 = p.provider("A1", session)
        val a2 = p.provider("A2", session)

        a1.name shouldBe "A1"
        a2.name shouldBe "A2"

        a1.service shouldBe "hello"
        a2.service shouldBe "hello"

        val a3 = assistedInjector("A3", session)
        a3.name shouldBe "A3"
        a3.service shouldBe "hello"
      }
  }
}

object AssistedInjectionTest extends LogSupport {
  type MyService = String

  trait NamedService {
    val name: String
    val service = bind[MyService]
  }

  trait NamedServiceProvider {
    val provider = (givenName: String, ss: Session) =>
      new NamedService with DISupport {
        override def session: Session = ss
        val name: String              = givenName
      }
  }

  def assistedInjector(serviceName: String, ss: Session): NamedService =
    new NamedService with DISupport {
      override def session: Session = ss
      val name: String              = serviceName
    }
}
