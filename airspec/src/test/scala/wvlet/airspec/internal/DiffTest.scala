package wvlet.airspec.internal

import wvlet.airspec.AirSpec
import wvlet.airspec.internal.diff.Diff

class DiffTest extends AirSpec{

  test("show diff") {
    val diff = new Diff("1", "2").createDiffOnlyReport()
    info(diff)
  }

  test("show long text diff") {
    val diff = new Diff(
      s"""select * from (
         |  select a, b, c from t1
         |  where t1.id = 1000
         )""".stripMargin,
      s"""select * from (
         |  select a, a, c from t1
         |  where t1.id = 1000
         |)""".stripMargin).createDiffOnlyReport()
    info(diff)
  }
}
