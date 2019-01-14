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

package wvlet.msgframe.sql

import java.io.File

import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.config.YamlReader
import wvlet.log.io.IOUtil

/**
  *
  */
object SQLBenchmark {

  case class TestQuery(sql: String, path: Option[String]) {
    override def toString = s"${name}:\n${sql}"
    def name              = path.getOrElse(s"${sql.substring(0, 20.min(sql.length))}")
  }

  private val RESOURCE_PATH = "msgframe-sql/src/test/resources/wvlet/msgframe/sql"

  private def readSQLFromYaml(path: String): Seq[TestQuery] = {
    val yaml = YamlReader.loadYamlList(path)

    yaml
      .map { y =>
        val msgpack = YamlReader.toMsgPack(y)
        val codec   = MessageCodecFactory.defaultFactory.of[TestQuery]
        codec.unpackMsgPack(msgpack).map { x =>
          x
        }
      }
      .filter(_.isDefined)
      .flatten
  }

  def allQueries: Seq[TestQuery] = {
    standardQueries ++ tpcH ++ tpcDS
  }

  def standardQueries: Seq[TestQuery] = {
    selection ++ ddl
  }

  lazy val selection: Seq[TestQuery] = {
    readSQLFromYaml(s"${RESOURCE_PATH}/standard/queries.yml")
  }

  lazy val ddl: Seq[TestQuery] = {
    readSQLFromYaml(s"${RESOURCE_PATH}/standard/ddl.yml")
  }

  private def readTestQueries(dir: File): Seq[TestQuery] = {
    for (f <- dir.listFiles() if f.getName.endsWith(".sql")) yield {
      TestQuery(IOUtil.readAsString(f.getPath), Some(f.getName))
    }
  }

  lazy val tpcDS: Seq[TestQuery] = {
    val dir = new File(s"${RESOURCE_PATH}/tpc-ds")
    readTestQueries(dir).filter { x =>
      // TODO support rollup operator
      !x.sql.toLowerCase.contains("rollup")
    }
  }

  def tpcDS_(q: String): TestQuery = {
    val f = new File(s"${RESOURCE_PATH}/tpc-ds/${q}.sql")
    TestQuery(IOUtil.readAsString(f.getPath), Some(f.getName))
  }

  lazy val tpcH: Seq[TestQuery] = {
    val dir = new File(s"${RESOURCE_PATH}/tpc-h")
    readTestQueries(dir)
  }
}
