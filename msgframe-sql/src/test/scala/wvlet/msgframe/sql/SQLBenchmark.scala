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

  case class TestQuery(sql: String)

  private val RESOURCE_PATH = "msgframe-sql/src/test/resources/wvlet/msgframe/sql"

  private def readSQLFromYaml(path: String): Seq[String] = {
    val yaml = YamlReader.loadYamlList(path)

    yaml
      .map { y =>
        val msgpack = YamlReader.toMsgPack(y)
        val codec   = MessageCodecFactory.defaultFactory.of[TestQuery]
        codec.unpackMsgPack(msgpack).map { x =>
          x.sql.trim
        }
      }
      .filter(_.isDefined)
      .flatten
  }

  def allQueries: Seq[String] = {
    standardQueries ++ tpcH ++ tpcDS
  }

  def standardQueries: Seq[String] = {
    selection ++ ddl
  }

  def selection: Seq[String] = {
    readSQLFromYaml(s"${RESOURCE_PATH}/standard/queries.yml")
  }

  def ddl: Seq[String] = {
    readSQLFromYaml(s"${RESOURCE_PATH}/standard/ddl.yml")
  }

  def tpcDS: Seq[String] = {
    val dir = new File(s"${RESOURCE_PATH}/tpc-ds")
    val sqls = for (f <- dir.listFiles() if f.getName.endsWith(".sql")) yield {
      IOUtil.readAsString(f.getPath)
    }
    sqls.filter { sql =>
      // TODO support rollup operator
      !sql.toLowerCase.contains("rollup")
    }
  }

  def tpcH: Seq[String] = {
    val dir = new File(s"${RESOURCE_PATH}/tpc-h")
    for (f <- dir.listFiles() if f.getName.endsWith(".sql")) yield {
      IOUtil.readAsString(f.getPath)
    }
  }
}
