package test

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
        info(s)
        if(str != expected) {
          warn(s"Surface: expected: ${expected}, but ${str}")
        }
    }
  }

  case class Person(id:Int, name:String)

  trait Label 
  type MyString = String

  def main(args: Array[String]): Unit = {
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
  }
  // ddddd

}
