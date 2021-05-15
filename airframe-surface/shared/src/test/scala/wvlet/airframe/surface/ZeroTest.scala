/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wvlet.airframe.surface

import wvlet.airframe.surface.tag.@@

/**
  */
class ZeroTest extends SurfaceSpec {
  scalaJsSupport

  import ZeroTest._

  protected def zeroCheck[P](surface: Surface, v: P): P = {
    val z = Zero.zeroOf(surface).asInstanceOf[P]
    surface match {
      case s: ArraySurface =>
        pendingUntil("array comparison")
      case _ =>
        z shouldBe v
    }
    z
  }

  test("support primitives") {
    zeroCheck(Surface.of[Unit], null)
    zeroCheck(Surface.of[Int], 0)
    zeroCheck(Surface.of[Long], 0L)
    zeroCheck(Surface.of[Char], 0.toChar)
    zeroCheck(Surface.of[Boolean], false)
    zeroCheck(Surface.of[Short], 0.toShort)
    zeroCheck(Surface.of[Byte], 0.toByte)
    zeroCheck(Surface.of[Float], 0f)
    zeroCheck(Surface.of[Double], 0.0)
    zeroCheck(Surface.of[String], "")
  }

  test("support arrays") {
    zeroCheck(Surface.of[Array[Int]], Array.empty[Int])
    zeroCheck(Surface.of[Array[Long]], Array.empty[Long])
    zeroCheck(Surface.of[Array[String]], Array.empty[String])
  }

  test("support Tuple") {
    zeroCheck(Surface.of[(Int, String)], (0, ""))
    zeroCheck(Surface.of[(Int, String, Seq[Int])], (0, "", Seq.empty))
  }

  test("special types") {
    zeroCheck(Surface.of[MyA], "")
    zeroCheck(Surface.of[Int @@ MyTag], 0)
    zeroCheck(Surface.of[Nothing], null)
    zeroCheck(Surface.of[AnyRef], null)
    zeroCheck(Surface.of[Any], null)
    zeroCheck(Surface.of[Option[String]], None)
  }

  test("support case classes") {
    val s = Surface.of[ZeroA]
    zeroCheck(Surface.of[ZeroA], ZeroA(0, "", ZeroB(0.0f, 0.0)))
    // Read the default parameter values.
    // Disabled the check because Scala.js doesn't support reading the default values:
    // https://github.com/wvlet/airframe/issues/149
    // zeroCheck(Surface.of[C], C(10, "Hello", 123.4f, B(0.0f, 0.0)))
  }

  test("support Scala collections") {
    zeroCheck(Surface.of[Seq[Int]], Seq.empty[Int])
    zeroCheck(Surface.of[IndexedSeq[Int]], IndexedSeq.empty[Int])
    zeroCheck(Surface.of[Map[Int, String]], Map.empty[Int, String])
    zeroCheck(Surface.of[Set[Int]], Set.empty[Int])
    zeroCheck(Surface.of[List[Int]], List.empty[Int])
  }
}

object ZeroTest {
  trait MyTag
  type MyA = String

  case class ZeroA(i: Int, s: String, b: ZeroB)
  case class ZeroB(f: Float, d: Double)
  //case class C(i: Int = 10, s: String = "Hello", f: Float = 123.4f, b: B)
}
