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

import java.io.{FileNotFoundException, FileOutputStream}
import java.util.Properties

import wvlet.airframe.config.PropertiesConfig.{ConfigKey, Prefix}
import wvlet.log.io.IOUtil
import wvlet.airframe.surface.tag._
import wvlet.airspec.AirSpec

trait AppScope
trait SessionScope

case class SampleConfig(id: Int, fullName: String)
case class DefaultConfig(id: Int = 1, a: String = "hello")
case class ClassConfig(classes: Seq[String], classAssignments: Map[String, String])

/**
  */
class ConfigTest extends AirSpec {
  val configPaths = Seq("airframe-config/src/test/resources")

  private def loadConfig(env: String) =
    Config(env = env, configPaths = configPaths)
      .registerFromYaml[SampleConfig]("myconfig.yml")
      .registerFromYaml[ClassConfig]("classes.yml")

  test("MapConfig should read map type configuration items") {
    val config      = loadConfig("development")
    val classConfig = config.of[ClassConfig]
    classConfig.classes.size shouldBe 3
    classConfig.classes(0) shouldBe "class1"
    classConfig.classes(1) shouldBe "class2"
    classConfig.classes(2) shouldBe "class3"

    classConfig.classAssignments.size shouldBe 3
    classConfig.classAssignments("nobita") shouldBe "class1"
    classConfig.classAssignments("takeshi") shouldBe "class2"
    classConfig.classAssignments("suneo") shouldBe "class3"
  }

  test("ConfigEnv should set config paths") {
    val env    = ConfigEnv("debug", "default", Seq.empty)
    val newEnv = env.withConfigPaths(Seq("."))
    newEnv.configPaths.contains(".") shouldBe true
  }

  test("cleanup config paths") {
    Config.cleanupConfigPaths(Seq("")).contains("") shouldBe false
    Config.cleanupConfigPaths(Seq("")).contains(".") shouldBe true
  }

  test("use current directory for search path") {
    val c = Config(env = "debug")
    c.env.configPaths.contains(".") shouldBe true
  }

  test("customize env") {
    val config    = Config(env = "staging", defaultEnv = "default")
    val devConfig = config.withEnv("development", "test")
    devConfig.env.env shouldBe "development"
    devConfig.env.defaultEnv shouldBe "test"
  }

  test("customize config paths") {
    val config    = Config(env = "staging", defaultEnv = "default", configPaths = Seq.empty)
    val newConfig = config.withConfigPaths(Seq("."))
    newConfig.env.configPaths.contains(".") shouldBe true
  }

  test("throw error on unknown config type") {
    val config = Config(env = "staging", defaultEnv = "default", configPaths = Seq.empty)
    intercept[IllegalArgumentException] {
      config.of[String]
    }
  }

  test("support getOrElse") {
    val config = Config(env = "staging", defaultEnv = "default", configPaths = Seq.empty).register[Int](10)
    val s      = config.getOrElse[String]("hello world")
    s shouldBe "hello world"

    config.getOrElse[Int](20) shouldBe 10
  }

  test("support registerFromYamlOrElse") {
    val config = Config(env = "staging", defaultEnv = "default", configPaths = Seq.empty)
      .registerFromYamlOrElse[String]("unknown-yaml-file.yml", "hello world")
    val s = config.of[String]
    s shouldBe "hello world"
  }

  test("map yaml file into a case class") {
    val config = loadConfig("default")

    val c1 = config.of[SampleConfig]
    c1.id shouldBe 1
    c1.fullName shouldBe "default-config"
  }

  test("read different env config") {
    val config = loadConfig("staging")

    val c = config.of[SampleConfig]
    c.id shouldBe 2
    c.fullName shouldBe "staging-config"
  }

  test("allow override") {
    val config = Config(env = "staging", configPaths = configPaths)
      .registerFromYaml[SampleConfig]("myconfig.yml").register[SampleConfig](SampleConfig(10, "hello"))

    val c = config.of[SampleConfig]
    c.id shouldBe 10
    c.fullName shouldBe "hello"
  }

  test("create a new config based on existing one") {
    val config = Config(env = "default", configPaths = configPaths).registerFromYaml[SampleConfig]("myconfig.yml")

    val config2 = Config(env = "production", configPaths = configPaths) + config

    val c2 = config2.of[SampleConfig]
    c2.id shouldBe 1
    c2.fullName shouldBe "default-config"
  }

  test("read tagged type") {
    val config = Config(env = "default", configPaths = configPaths)
      .registerFromYaml[SampleConfig @@ AppScope]("myconfig.yml")
      .register[SampleConfig @@ SessionScope](SampleConfig(2, "session").asInstanceOf[SampleConfig @@ SessionScope])

    val c = config.of[SampleConfig @@ AppScope]
    c shouldBe SampleConfig(1, "default-config")

    val s = config.of[SampleConfig @@ SessionScope]
    s shouldBe SampleConfig(2, "session")
  }

  test("throw exception on missing environment") {
    intercept[IllegalArgumentException] {
      val config = Config(env = "weird-env", defaultEnv = "unknown", configPaths = configPaths)
        .registerFromYaml[SampleConfig]("myconfig.yml")
    }
  }

  test("register the default objects") {
    val config = Config(env = "default").registerDefault[DefaultConfig].registerDefault[SampleConfig]

    config.of[DefaultConfig] shouldBe DefaultConfig()
    // Sanity test
    config.of[SampleConfig] shouldBe SampleConfig(0, "")

    // Override the default object with properties
    val p = new Properties
    p.setProperty("default.a", "world")
    val c2 = config.overrideWithProperties(p).of[DefaultConfig]
    c2 shouldBe DefaultConfig(1, "world")
  }

  test("show the default configuration") {
    val config = Config(env = "default", configPaths = configPaths).registerFromYaml[SampleConfig]("myconfig.yml")

    val default = config.defaultValueOf[SampleConfig]
    val current = config.of[SampleConfig]

    info(s"default: ${default}\ncurrent: ${current}")

    val changes = config.getConfigChanges
    changes.size shouldBe 2
    info(s"changes:\n${changes.mkString("\n")}")
    val keys: Seq[String] = changes.map(_.key.toString)
    keys.contains("sample.id") shouldBe true
    keys.contains("sample.fullname") shouldBe true

    val id = changes.find(_.key.toString == "sample.id").get
    id.default shouldBe 0
    id.current shouldBe 1

    val fullname = changes.find(_.key.toString == "sample.fullname").get
    fullname.default shouldBe ""
    fullname.current shouldBe "default-config"
  }

  test("override values with properties") {
    val p = new Properties
    p.setProperty("sample.id", "10")
    p.setProperty("sample@appscope.id", "2")
    p.setProperty("sample@appscope.full_name", "hellohello")

    val c1 = Config(env = "default", configPaths = configPaths)
      .register[SampleConfig](SampleConfig(1, "hello"))
      .register[SampleConfig @@ AppScope](SampleConfig(1, "hellohello").asInstanceOf[SampleConfig @@ AppScope])
      .overrideWithProperties(p)

    class ConfigSpec(config: Config) {
      val c = config.of[SampleConfig]
      c shouldBe SampleConfig(10, "hello")

      val c2 = config.of[SampleConfig @@ AppScope]
      c2 shouldBe SampleConfig(2, "hellohello")
    }

    new ConfigSpec(c1)

    // Using properties files
    IOUtil.withTempFile("config", dir = "target") { file =>
      val f = new FileOutputStream(file)
      p.store(f, "config prop")
      f.close()

      val c2 = Config(env = "default", configPaths = Seq("target"))
        .register[SampleConfig](SampleConfig(1, "hello"))
        .register[SampleConfig @@ AppScope](SampleConfig(1, "hellohello"))
        .overrideWithPropertiesFile(file.getName)

      new ConfigSpec(c2)
    }
  }

  test("parse configuration property keys") {
    PropertiesConfig.configKeyOf("tpe.param") shouldBe ConfigKey(Prefix("tpe", None), "param")
    PropertiesConfig.configKeyOf("tpe@tag.param") shouldBe ConfigKey(Prefix("tpe", Some("tag")), "param")
    PropertiesConfig.configKeyOf("tpe@@tag.param") shouldBe ConfigKey(Prefix("tpe", Some("tag")), "param")
    // Allow hyphenated names
    PropertiesConfig.configKeyOf("t-pe@@tag.param") shouldBe ConfigKey(Prefix("tpe", Some("tag")), "param")
    PropertiesConfig.configKeyOf("t-pe@@t-ag.param") shouldBe ConfigKey(Prefix("tpe", Some("tag")), "param")

    intercept[IllegalArgumentException] {
      PropertiesConfig.configKeyOf("tpe@@param")
    }
  }

  test("find unused properties") {
    val p = new Properties
    p.setProperty("sample.id", "10")
    p.setProperty("sample@appscope.id", "2")
    p.setProperty("sample@appscope.message", "hellohello") // should be unused

    var unused: Option[Properties] = None
    val c = Config(env = "default", configPaths = configPaths)
      .register[SampleConfig](SampleConfig(1, "hello"))
      .register[SampleConfig @@ AppScope](SampleConfig(1, "hellohello").asInstanceOf[SampleConfig @@ AppScope])
      .overrideWithProperties(p, onUnusedProperties = { p: Properties => unused = Some(p) })

    unused shouldBe defined
    unused.get.size shouldBe 1
    unused.get.keySet.contains("sample@appscope.message") shouldBe true
  }

  test("report missing Properties file error") {
    intercept[FileNotFoundException] {
      val c = Config(env = "default", configPaths = configPaths)
        .overrideWithPropertiesFile("unknown-propertiles-file-path.propertiles")
    }
  }

  test("report error if unused Properties are found") {
    intercept[IllegalArgumentException] {
      val p = new Properties()
      p.setProperty("sample.idid", "10")
      val c = Config(env = "default", configPaths = configPaths)
        .overrideWithProperties(p, onUnusedProperties = Config.REPORT_ERROR_FOR_UNUSED_PROPERTIES)
    }
  }

  test("report missing YAML file error") {
    intercept[FileNotFoundException] {
      val c =
        Config(env = "default", configPaths = configPaths).registerFromYaml[SampleConfig]("myconfig-missing.yml")
    }
  }

  test("report missing value error") {
    intercept[Exception] {
      val c = Config(env = "unknown-env", defaultEnv = "unknown-default", configPaths = configPaths)
        .registerFromYaml[SampleConfig]("myconfig.yml")
    }
  }
}
