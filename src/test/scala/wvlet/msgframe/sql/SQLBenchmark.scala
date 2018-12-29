package wvlet.msgframe.sql

import java.io.File

import wvlet.log.io.IOUtil

/**
  *
  */
object SQLBenchmark {

  def tpcDS: Seq[String] = {
    val dir = new File("msgframe-sql/src/test/resources/wvlet/msgframe/sql/tpc-ds")
    for (f <- dir.listFiles() if f.getName.endsWith(".sql")) yield {
      IOUtil.readAsString(f.getPath)
    }
  }

  def tpcH: Seq[String] = {
    val dir = new File("msgframe-sql/src/test/resources/wvlet/msgframe/sql/tpc-h")
    for (f <- dir.listFiles() if f.getName.endsWith(".sql")) yield {
      IOUtil.readAsString(f.getPath)
    }
  }

}
