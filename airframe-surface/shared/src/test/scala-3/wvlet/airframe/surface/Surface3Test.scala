package test

import wvlet.airframe.surface.Surface

object Surface3Test {
  import scala.quoted._


  inline def test(s:Surface): Unit = {
    println(s"surface: ${s}")
  }

  case class Person(id:Int, name:String)


  def main(args: Array[String]): Unit = {
    test(Surface.of[Int])
    test(Surface.of[Long])
    test(Surface.of[String])
    test(Surface.of[Seq[Int]])
    test(Surface.of[Person])
    //  abcddddwddddd
  }
  // ddddddddddddd

}
