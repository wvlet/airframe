package dotty.test

import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport
import wvlet.airframe.surface.tag._

object Surface3Test extends LogSupport {
  import scala.quoted._

  inline def test(s:Surface, expected: String): Unit = {
    s match {
      case null =>
        warn(s"Surface: expected: ${expected}, but null")
      case _ =>
        val str = s.toString
        // info(s"${s}: ${s.getClass}")
        if(str != expected) {
          warn(s"Surface: expected: ${expected}, but ${str}")
        }
    }
  }

  inline def assert(v: Any, expected: Any): Unit = {
    if(v != expected) {
      warn(s"Expected: ${expected}, but ${v}")
    }
  }

  case class Person(id:Int = -1, name:String)

  trait Label 
  type MyString = String

  trait Holder[M[_]] {
    def hello = "hello"
  }

  trait Task[A]

  class Hello {
    def hello(msg:String): String = msg
    private def hiddemMethod: Int = 1
    protected def hiddenMethod2: Option[Int] = None
  }

  class HelloExt extends Hello {
    def hello2: String = "hello2"
  }

  case class MyOpt(a:Option[String])

  def run:Unit = {
    test(Surface.of[Int], "Int")
    test(Surface.of[Boolean], "Boolean")
    test(Surface.of[Long], "Long")
    test(Surface.of[Float], "Float")
    test(Surface.of[Double], "Double")
    test(Surface.of[Short], "Short")
    test(Surface.of[Char], "Char")
    test(Surface.of[Unit], "Unit")
    test(Surface.of[String], "String")
    test(Surface.of[Seq[Int]], "Seq[Int]")
    test(Surface.of[Map[String, Int]], "Map[String,Int]")
    test(Surface.of[Person], "Person")
    test(Surface.of[String @@ Label], "String@@Label")
    test(Surface.of[MyString], "MyString:=String")
    test(Surface.of[Array[Int]], "Array[Int]")
    test(Surface.of[Array[Person]], "Array[Person]")
    test(Surface.of[Option[String]], "Option[String]")
    test(Surface.of[(Int, String)], "Tuple2[Int,String]")
    test(Surface.of[(Int, String, Float)], "Tuple3[Int,String,Float]")
    test(Surface.of[java.io.File], "File")
    test(Surface.of[java.util.Date], "Date")
    test(Surface.of[java.time.temporal.Temporal], "Temporal")
    test(Surface.of[MyEnum], "MyEnum")
    test(Surface.of[Seq[_]], "Seq[_]")
    // TODO　bounded type support
    test(Surface.of[Seq[_ <: String]], "Seq[_]")
    test(Surface.of[Seq[_ >: String]], "Seq[_]")
    test(Surface.of[Holder[Task]], "Holder[Task]")

    test(Surface.of[Label], "Label")

    // Case class surface tests
    val s = Surface.of[Person]
    assert(s.params.mkString(","), "id:Int,name:String")
    val p = Person(1, "leo")
    val p0 = s.params(0)
    assert(p0.name, "id")
    assert(p0.getDefaultValue, Some(-1))
    assert(p0.get(p), 1)
    val px = s.objectFactory.map(_.newInstance(Seq(2, "yui")))
    assert(px, Some(Person(2, "yui")))

    val ms = Surface.methodsOf[Hello]
    debug(ms)
    val h = new Hello
    val res = ms(0).call(h, "hello surface")
    assert(res, "hello surface")

    val ms2 = Surface.methodsOf[HelloExt]
    debug(ms2)
    val res2 = ms2.find(_.name == "hello2").map { x =>
      x.call(new HelloExt)
    }
    assert(res2, Some("hello2"))

    val sc = Surface.of[MyOpt]
    info(sc.params)
  }


}
