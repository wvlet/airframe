package examples

import wvlet.airspec.AirSpec

class CustomDesignTest extends AirSpec {

  initDesign {
    _.bind[String]
      .toInstance("custom string")
      .bind[Int].toInstance(10)
  }

  initLocalDesign {
    _.bind[Int].toInstance(1000)
  }

  test("customDesign") { (s: String, i: Int) =>
    s shouldBe "custom string"
    i shouldBe 1000
  }
}
