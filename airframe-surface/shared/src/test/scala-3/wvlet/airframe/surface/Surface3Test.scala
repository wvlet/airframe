package test

import wvlet.airframe.surface.Surface
import scala.reflect.ClassTag

object Surface3Test {
  import scala.quoted._

  inline def test(s:Surface): Unit = {
    println(s"surface: ${s}")
  }

  def foo[T: ClassTag] = {
    val t = implicitly[ClassTag[T]]
    println(t)
    println(t.runtimeClass)
  }

  def main(args: Array[String]): Unit = {
    test(Surface.of[Int])
    test(Surface.of[Long])
    test(Surface.of[String])
    test(Surface.of[Seq[Int]])
    foo[Map[Int, String]]
    //  abcddddwddd
  }
  // dddd

}
