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

case class AppConfig(name: String)
case class App2Config(name: String)

object AirframeModuleTest {
  lazy val module1 =
    AirframeModule.newModule
      .withConfig { c =>
        c.register(AppConfig("hello"))
      }
      .withDesign { d =>
        d.bind[String].toInstance("world")
      }

  lazy val module2 =
    AirframeModule.newModule
      .withDesign { d =>
        d.bind[String].toInstance("Airframe")
      }

  lazy val module3 =
    AirframeModule.newModule
      .withConfig { c =>
        c.register(App2Config("scala"))
      }
}

import wvlet.airframe.bootstrap.AirframeModuleTest._

/**
  *
  */
class AirframeModuleTest extends AirframeSpec {
  "AirframeModule" should {
    "start" in {

      val b = AirframeBootstrap(module1)
      b.bootstrap.withSession { session =>
        session.build[AppConfig] shouldBe AppConfig("hello")
        session.build[String] shouldBe "world"
      }
    }

    "combine modules" in {
      val b = AirframeBootstrap(module1 + module2)
      b.bootstrap.withSession { session =>
        session.build[AppConfig] shouldBe AppConfig("hello")
        session.build[String] shouldBe "Airframe"
      }
    }

    "override config" in {
      val b = AirframeBootstrap(module1 + module3)
        .withConfigOverrides(Map("app.name" -> "good morning"))

      b.bootstrap.withSession { session =>
        session.build[AppConfig] shouldBe AppConfig("good morning")
        session.build[App2Config] shouldBe App2Config("scala")
      }
    }
  }
}
