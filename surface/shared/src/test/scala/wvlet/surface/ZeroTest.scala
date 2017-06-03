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
      zeroCheck(Surface.of[Int], 0)
      zeroCheck(Surface.of[Long], 0L)
      zeroCheck(Surface.of[Boolean], false)
      zeroCheck(Surface.of[Short], 0.toShort)
      zeroCheck(Surface.of[Byte], 0.toByte)
      zeroCheck(Surface.of[Float], 0f)
      zeroCheck(Surface.of[Double], 0.0)
      zeroCheck(Surface.of[String], "")
      zeroCheck(Surface.of[Unit], null)
    }

    "support arrays" in {
      zeroCheck(Surface.of[Array[Int]], Array.empty[Int])
      zeroCheck(Surface.of[Array[Long]], Array.empty[Long])
      zeroCheck(Surface.of[Array[String]], Array.empty[String])
    }

    "support Tuple" in {
      zeroCheck(Surface.of[(Int, String)], (0, ""))
      zeroCheck(Surface.of[(Int, String, Seq[Int])], (0, "", Seq.empty))
    }

    "special types" in {
      zeroCheck(Surface.of[Option[String]], None)
      zeroCheck(Surface.of[MyA], "")
      zeroCheck(Surface.of[Int @@ MyTag], 0)
      zeroCheck(Surface.of[Nothing], null)
      zeroCheck(Surface.of[AnyRef], null)
      zeroCheck(Surface.of[Any], null)
    }

    "support case classes" in {
      zeroCheck(Surface.of[A], A(0, "", B(0.0f, 0.0)))
    }

    "support Scala collections" in {
      zeroCheck(Surface.of[Seq[Int]], Seq.empty[Int])
      zeroCheck(Surface.of[IndexedSeq[Int]], IndexedSeq.empty[Int])
      zeroCheck(Surface.of[Map[Int,String]], Map.empty[Int, String])
      zeroCheck(Surface.of[Set[Int]], Set.empty[Int])
      zeroCheck(Surface.of[List[Int]], List.empty[Int])
    }
  }
}

object ZeroTest {

  trait MyTag
  type MyA = String

  case class A(i:Int, s:String, b:B)
  case class B(f:Float, d:Double)
}
