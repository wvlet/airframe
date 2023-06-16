package examples

import wvlet.airspec.AirSpec

class SingleExecutionTest extends AirSpec {

  test("t1") {
    debug("run t1")
    Nil
  }

  test("t2") {
    debug("run t2....")
    Seq(1)
  }
}
