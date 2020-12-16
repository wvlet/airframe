package test

import wvlet.airframe.surface.Surface

object Surface3Test {
  def main(args: Array[String]): Unit = {
    val s = Surface.of[Int]
    println(s"surface: ${s}")
  }
}
