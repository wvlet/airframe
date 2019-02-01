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
package wvlet.airframe.config

import wvlet.airframe.AirframeSpec
import wvlet.airframe.surface._

object AirframeBootstrapTest {
  case class AppConfig(name: String)
  case class App2Config(name: String)

  import wvlet.airframe._

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
      module1.noLifeCycleLogging.showConfig
        .withSession { session =>
          session.build[AppConfig] shouldBe AppConfig("hello")
          session.build[String] shouldBe "world"
        }
    }

    "combine modules" in {
      (module1 + module2).noLifeCycleLogging.showConfig
        .withSession { session =>
          session.build[AppConfig] shouldBe AppConfig("hello")
          session.build[String] shouldBe "Airframe"
        }
    }

    "override config" in {
      (module1 + module3).noLifeCycleLogging
        .overrideConfigParams(Map("app.name" -> "good morning"))
        .showConfig
        .withSession { session =>
          session.build[AppConfig] shouldBe AppConfig("good morning")
          session.build[App2Config] shouldBe App2Config("scala")
        }
    }

    "get config" in {
      module3.noLifeCycleLogging.getConfig match {
        case Some(c) =>
          c.getAll.length shouldBe 1
          c.getAll.head.tpe shouldBe Surface.of[App2Config]
          c.getAll.head.value shouldBe App2Config("scala")
        case None =>
          fail()
      }
    }
  }
}
