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
package wvlet.airframe.codec
import wvlet.airframe.surface.Surface

/**
  */
class ScalaStandardCodecTest extends CodecSpec {
  scalaJsSupport

  def `support Option[A]` : Unit = {
    val v = Some("hello")
    roundtrip(Surface.of[Option[String]], Some("hello"))
    roundtrip[Option[String]](Surface.of[Option[String]], None)
    roundtrip[Option[Int]](Surface.of[Option[Int]], None)
    roundtrip[Option[Seq[Int]]](Surface.of[Option[Seq[Int]]], Some(Seq(1, 2, 3)))
  }

  def `support tuple`: Unit = {
    roundtrip[Tuple1[String]](Surface.of[Tuple1[String]], Tuple1("hello"))
    roundtrip(Surface.of[(Int, Int)], (1, 2))
    roundtrip(Surface.of[(Int, Int, Int)], (1, 2, 3))
    roundtrip(Surface.of[(Int, String, Boolean, Float)], (1, "a", true, 2.0f))
    roundtrip(Surface.of[(Int, String, Boolean, Float, String)], (1, "a", true, 2.0f, "hello"))
    roundtrip(Surface.of[(Int, String, Boolean, Float, String, Seq[Int])], (1, "a", true, 2.0f, "hello", Seq(1, 3, 4)))

    roundtrip(Surface.of[(Int, Int, Int, Int, Int, Int, Int)], (1, 2, 3, 4, 5, 6, 7))
    roundtrip(Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int)], (1, 2, 3, 4, 5, 6, 7, 8))
    roundtrip(Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int)], (1, 2, 3, 4, 5, 6, 7, 8, 9))
    roundtrip(Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)], (1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
    roundtrip(Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)], (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
    roundtrip(
      Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
    )
    roundtrip(
      Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)
    )
    roundtrip(
      Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
    )
    roundtrip(
      Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
    )
    roundtrip(
      Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
    )
    roundtrip(
      Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)
    )
    roundtrip(
      Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18)
    )
    roundtrip(
      Surface
        .of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19)
    )
    roundtrip(
      Surface
        .of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
    )
    roundtrip(
      Surface
        .of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21)
    )
  }
}
