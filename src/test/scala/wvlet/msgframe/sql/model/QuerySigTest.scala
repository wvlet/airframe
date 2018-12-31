package wvlet.msgframe.sql.model
import wvlet.airframe.AirframeSpec
import wvlet.msgframe.sql.SQLBenchmark

/**
  *
  */
class QuerySigTest extends AirframeSpec {

  "Find input/output tables" in {
    for (sql <- SQLBenchmark.allQueries) {
      val g = QuerySig.findInputOutputTableGraph(sql)
      debug(g)
    }
  }

  "Generate signature" in {
    SQLBenchmark.allQueries.foreach { sql =>
      val s = QuerySig.sig(sql)
      debug(s)
    }
  }
}
