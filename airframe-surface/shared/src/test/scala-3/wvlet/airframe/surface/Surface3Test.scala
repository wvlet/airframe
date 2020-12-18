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
    test(Surface.of[Long], "Long")
    test(Surface.of[String], "String")
    test(Surface.of[Seq[Int]], "Seq[Int]")
    test(Surface.of[Map[String, Int]], "Map[String,Int]")
    test(Surface.of[Person], "Person")
    test(Surface.of[String @@ Label], "String@@Label")
    test(Surface.of[MyString], "MyString:=String")
  }
  // ddddd

}
