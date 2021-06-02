package wvlet.airframe.surface

import wvlet.airspec.AirSpec

object AbstractTypeTest extends AirSpec {

  trait Abst {
    def hello = "hello abst"
  }
  class AbstImpl extends Abst {
    override def hello: String = "hello impl"
  }

  test("object factory of an abstract type impl") {
    val s = Surface.of[AbstImpl]
    s.objectFactory shouldBe defined

    val a = s.objectFactory.get.newInstance(Seq.empty).asInstanceOf[Abst]
    a.hello shouldBe "hello impl"
  }
}
