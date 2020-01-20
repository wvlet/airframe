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

import wvlet.airframe.Design
import wvlet.airframe.surface._
import wvlet.airspec.AirSpec

object AirframeBootstrapTest {
  case class AppConfig(name: String)
  case class App2Config(name: String)
  case class DBConfig(host: String, private val port: Option[Int] = None)

  import wvlet.airframe._

  val module1 =
    newDesign
      .bindConfig(AppConfig("hello"))
      .bindConfig(DBConfig("localhost"))
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
class AirframeBootstrapTest extends AirSpec {
  import AirframeBootstrapTest._

  def `bind configs`: Unit = {
    module1.noLifeCycleLogging.showConfig
      .withSession { session =>
        session.build[AppConfig] shouldBe AppConfig("hello")
        session.build[String] shouldBe "world"
      }
  }

  def `combine modules`: Unit = {
    (module1 + module2).noLifeCycleLogging.showConfig
      .withSession { session =>
        session.build[AppConfig] shouldBe AppConfig("hello")
        session.build[String] shouldBe "Airframe"
      }
  }

  def `override config`: Unit = {
    (module1 + module3).noLifeCycleLogging
      .overrideConfigParams(Map("app.name" -> "good morning"))
      .showConfig
      .withSession { session =>
        session.build[AppConfig] shouldBe AppConfig("good morning")
        session.build[App2Config] shouldBe App2Config("scala")
      }
  }

  def `get config`: Unit = {
    module3.noLifeCycleLogging.getConfig match {
      case Some(c) =>
        c.getAll.length shouldBe 1
        c.getAll.head.tpe shouldBe Surface.of[App2Config]
        c.getAll.head.value shouldBe App2Config("scala")
      case None =>
        fail()
    }
  }

  def `hide credentials`: Unit = {
    val x = Credential("leo", "hidden secret password", "some important information", Some("hidden secret key"))
    Design.newSilentDesign
      .bindConfig[Credential](x)
      .showConfig
  }
}

case class Credential(
    user: String,
    @secret password: String,
    @secret(mask = true) key: String,
    @secret secretKey: Option[String]
)
