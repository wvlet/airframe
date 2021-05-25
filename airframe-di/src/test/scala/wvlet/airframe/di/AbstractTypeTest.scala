package wvlet.airframe.di

import wvlet.airspec.AirSpec
import wvlet.airframe.Design

object AbstractTypeTest extends AirSpec {
  
  trait Abst {
    def hello = "hello abst"
  }
  class AbstImpl extends Abst {
    override def hello: String = "hello impl"
  }

  test("bind to abstract type") {
    val d = Design.newSilentDesign
      .bind[Abst].to[AbstImpl]

    d.build[Abst] { (a: Abst) =>
      a.hello shouldBe "hello impl"
    }
  }

}
