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
package wvlet.config

import wvlet.log.io.Resource
import wvlet.test.WvletSpec

case class MyConfig(id: Int, fullName: String)
case class DB(accountId: Int, database: String, table: Seq[String])

/**
  *
  */
class YamlReaderTest extends WvletSpec {

  val yml = Resource.find("myconfig.yml").map(_.getPath).getOrElse {
    fail("myconfig.yml is not found")
  }
  val listYml = Resource.find("list.yml").map(_.getPath).getOrElse {
    fail("list.yml is not found")
  }

  val classesYml = Resource.find("classes.yml").map(_.getPath).getOrElse {
    fail("classes.yml is not found")
  }

  "YamlReader" should {
    "parse yaml file" in {
      val m = YamlReader.loadYaml(yml)
      m.keys should contain("default")
      m.keys should contain("staging")
      m.keys should have size (2)
    }

    "read yaml as objects" in {
      val m = YamlReader.loadMapOf[MyConfig](yml)
      m.keys should contain("default")
      m.keys should contain("staging")

      m("default") shouldBe MyConfig(1, "default-config")
      m("staging") shouldBe MyConfig(2, "staging-config")
    }

    "read an specific env from yaml" in {
      val m = YamlReader.load[MyConfig](yml, "staging")
      m shouldBe MyConfig(2, "staging-config")
    }

    "throw an exception when the target env is missing" in {
      intercept[IllegalArgumentException] {
        YamlReader.load[MyConfig](yml, "production")
      }
    }

    "parse lists in yaml" in {
      val m = YamlReader.loadYamlList(listYml)
      m should have size (2)
      m(0)("database") shouldBe "mydb"
      m(0)("account_id") shouldBe 1
      m(1)("database") shouldBe "mydb2"
      m(1)("account_id") shouldBe 10

      val s = m.map(p => YamlReader.bind[DB](p))
      s(0) shouldBe DB(1, "mydb", Seq("A"))
      s(1) shouldBe DB(10, "mydb2", Seq("T1", "T2"))
    }

    "parse map in yaml" taggedAs ("map") in {
      val m = YamlReader.loadMapOf[ClassConfig](classesYml)
      m should have size (2)
      m("development").classes shouldBe Seq("class1", "class2", "class3")
      m("development").classAssignments shouldBe Map(
        "nobita"  -> "class1",
        "takeshi" -> "class2",
        "suneo"   -> "class3"
      )
    }
  }
}
