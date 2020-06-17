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

import wvlet.airspec.AirSpec
import wvlet.log.io.Resource
import java.io.FileNotFoundException

case class MyConfig(id: Int, fullName: String, port: Int = 8989)
case class DB(accountId: Int, database: String, table: Seq[String])

/**
  */
class YamlReaderTest extends AirSpec {

  private def findFile(name: String): String = {
    Resource.find(name) match {
      case Some(x) => x.getPath
      case None =>
        throw new FileNotFoundException(s"${name} is not found")
    }
  }

  val yml: String        = findFile("myconfig.yml")
  val listYml: String    = findFile("list.yml")
  val classesYml: String = findFile("classes.yml")

  def `parse yaml file`: Unit = {
    val m = YamlReader.loadYaml(yml)
    m.contains("default") shouldBe true
    m.contains("staging") shouldBe true
    m.size shouldBe 2
  }

  def `read yaml as objects`: Unit = {
    val m = YamlReader.loadMapOf[MyConfig](yml)
    m.contains("default") shouldBe true
    m.contains("staging") shouldBe true

    m("default") shouldBe MyConfig(1, "default-config", 8989)
    m("staging") shouldBe MyConfig(2, "staging-config", 10000)
  }

  def `read an specific env from yaml`: Unit = {
    val m = YamlReader.load[MyConfig](yml, "staging")
    m shouldBe MyConfig(2, "staging-config", 10000)
  }

  def `throw an exception when the target env is missing`: Unit = {
    intercept[IllegalArgumentException] {
      YamlReader.load[MyConfig](yml, "production")
    }
  }

  def `parse lists in yaml`: Unit = {
    val m = YamlReader.loadYamlList(listYml)
    m.size shouldBe 2
    m(0)("database") shouldBe "mydb"
    m(0)("account_id") shouldBe 1
    m(1)("database") shouldBe "mydb2"
    m(1)("account_id") shouldBe 10

    val s = m.map(p => YamlReader.bind[DB](p))
    s(0) shouldBe DB(1, "mydb", Seq("A"))
    s(1) shouldBe DB(10, "mydb2", Seq("T1", "T2"))
  }

  def `parse map in yaml`: Unit = {
    val m = YamlReader.loadMapOf[ClassConfig](classesYml)
    m.size shouldBe 2
    m("development").classes shouldBe Seq("class1", "class2", "class3")
    m("development").classAssignments shouldBe Map(
      "nobita"  -> "class1",
      "takeshi" -> "class2",
      "suneo"   -> "class3"
    )
  }
}
