package wvlet.airspec.internal

import wvlet.airspec.AirSpec
import wvlet.airspec.internal.diff.{Diff, DiffUtils}

object DiffTest extends AirSpec{

  test("show diff") {
    val diff = new Diff("1", "2").createDiffOnlyReport()
    info(diff)
  }

  test("show long text diff") {
    val diff = new Diff(
      s"""select * from (
         |  select a, b, c from t1
         |  where t1.id = 1000
         |)""".stripMargin,
      s"""select * from (
         |  select a, a, c from t1
         |  where t1.id = 1000
         |)""".stripMargin).createDiffOnlyReport()
    info(diff)
  }

  case class Record(id:Int, name:String)

  test("case class diff") {
    val r1 = PrettyPrint.pp(Record(1, "leo"))
    val r2 = PrettyPrint.pp(Record(2, "leo"))

    val report = new Diff(r1, r2).createDiffOnlyReport()
    info(report)
  }
}
