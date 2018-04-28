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

import wvlet.airframe.{AirframeSpec, Design}
import wvlet.config.Config

case class AppConfig(name: String)
case class App2Config(name: String)

object Module1 extends AirframeModule {
  override def configure(config: Config): Config = {
    config.register(AppConfig("hello"))
  }

  override def design(d: Design): Design = {
    d.bind[String].toInstance("world")
  }
}

object Module2 extends AirframeModule {
  override def design(d: Design): Design = {
    d.bind[String].toInstance("Airframe")
  }
}

object Module3 extends AirframeModule {
  override def configure(config: Config): Config = {
    config.register(App2Config("scala"))
  }
}

/**
  *
  */
class AirframeModuleTest extends AirframeSpec {
  "AirframeModule" should {
    "start" in {

      Module1.bootstrap().main { session =>
        session.build[AppConfig] shouldBe AppConfig("hello")
        session.build[String] shouldBe "world"
      }
    }

    "combine modules" in {
      val m = Module1 + Module2
      m.bootstrap().main { session =>
        session.build[AppConfig] shouldBe AppConfig("hello")
        session.build[String] shouldBe "Airframe"
      }
    }

    "override config" in {
      val m = Module1 + Module3
      m.bootstrap(overrideConfigParams = Map("app.name" -> "good morning")).main { session =>
        session.build[AppConfig] shouldBe AppConfig("good morning")
        session.build[App2Config] shouldBe App2Config("scala")
      }
    }
  }
}
