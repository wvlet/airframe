package wvlet.msgframe.sql.model
import wvlet.airframe.AirframeSpec
import wvlet.msgframe.sql.SQLBenchmark

/**
  *
  */
class QuerySigTest extends AirframeSpec {

  "Find input/output tables" in {
    for (sql <- SQLBenchmark.standardQueries ++ SQLBenchmark.tpcH ++ SQLBenchmark.tpcDS) {
      val g = QuerySig.findInputOutputTableGraph(sql)
      info(g)
    }
  }

}
