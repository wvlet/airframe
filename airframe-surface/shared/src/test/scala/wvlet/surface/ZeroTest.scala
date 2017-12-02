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

package wvlet.surface

import wvlet.surface.tag.@@
import wvlet.surface

/**
  *
  */
class ZeroTest extends SurfaceSpec {

  import Zero._
  import ZeroTest._

  def zeroCheck[A](surface: Surface, v: A): A = {
    val z = zeroOf(surface).asInstanceOf[A]
    z shouldBe v
    z
  }

  "Zero" should {
    "support primitives" in {
      zeroCheck(surface.of[Int], 0)
      zeroCheck(surface.of[Long], 0L)
      zeroCheck(surface.of[Char], 0.toChar)
      zeroCheck(surface.of[Boolean], false)
      zeroCheck(surface.of[Short], 0.toShort)
      zeroCheck(surface.of[Byte], 0.toByte)
      zeroCheck(surface.of[Float], 0f)
      zeroCheck(surface.of[Double], 0.0)
      zeroCheck(surface.of[String], "")
      zeroCheck(surface.of[Unit], null)
    }

    "support arrays" in {
      zeroCheck(surface.of[Array[Int]], Array.empty[Int])
      zeroCheck(surface.of[Array[Long]], Array.empty[Long])
      zeroCheck(surface.of[Array[String]], Array.empty[String])
    }

    "support Tuple" in {
      zeroCheck(surface.of[(Int, String)], (0, ""))
      zeroCheck(surface.of[(Int, String, Seq[Int])], (0, "", Seq.empty))
    }

    "special types" in {
      zeroCheck(surface.of[Option[String]], None)
      zeroCheck(surface.of[MyA], "")
      zeroCheck(surface.of[Int @@ MyTag], 0)
      zeroCheck(surface.of[Nothing], null)
      zeroCheck(surface.of[AnyRef], null)
      zeroCheck(surface.of[Any], null)
    }

    "support case classes" in {
      zeroCheck(surface.of[A], A(0, "", B(0.0f, 0.0)))
    }

    "support Scala collections" in {
      zeroCheck(surface.of[Seq[Int]], Seq.empty[Int])
      zeroCheck(surface.of[IndexedSeq[Int]], IndexedSeq.empty[Int])
      zeroCheck(surface.of[Map[Int, String]], Map.empty[Int, String])
      zeroCheck(surface.of[Set[Int]], Set.empty[Int])
      zeroCheck(surface.of[List[Int]], List.empty[Int])
    }
  }
}

object ZeroTest {

  trait MyTag
  type MyA = String

  case class A(i: Int, s: String, b: B)
  case class B(f: Float, d: Double)
}
