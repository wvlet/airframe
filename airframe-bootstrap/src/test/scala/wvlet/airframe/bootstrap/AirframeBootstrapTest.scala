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
package wvlet.airframe.bootstrap
import wvlet.airframe.AirframeSpec

object AirframeBootstrapTest {
  import wvlet.airframe._
  import wvlet.airframe.config._

  val module1 =
    newDesign
      .bindConfig(AppConfig("hello"))
      .bind[String].toInstance("world")

  val module2 =
    newDesign
      .bind[String].toInstance("Airframe")

  val module3 =
    newDesign
      .bindConfig(App2Config("scala"))
}

/**
  *
  */
class AirframeBootstrapTest extends AirframeSpec {
  import AirframeBootstrapTest._

  "AirframeBootstrap" should {
    "bind configs" in {
      module1.withSession { session =>
        session.build[AppConfig] shouldBe AppConfig("hello")
        session.build[String] shouldBe "world"
      }
    }

  }

}
