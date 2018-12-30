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

  def standardQueries: Seq[String] = {
    val yaml = YamlReader.loadYamlList("msgframe-sql/src/test/resources/wvlet/msgframe/sql/standard/queries.yml")

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

  def tpcDS: Seq[String] = {
    val dir = new File("msgframe-sql/src/test/resources/wvlet/msgframe/sql/tpc-ds")
    val sqls = for (f <- dir.listFiles() if f.getName.endsWith(".sql")) yield {
      IOUtil.readAsString(f.getPath)
    }
    sqls.filter { sql =>
      // TODO support rollup operator
      !sql.toLowerCase.contains("rollup")
    }
  }

  def tpcH: Seq[String] = {
    val dir = new File("msgframe-sql/src/test/resources/wvlet/msgframe/sql/tpc-h")
    for (f <- dir.listFiles() if f.getName.endsWith(".sql")) yield {
      IOUtil.readAsString(f.getPath)
    }
  }
}
