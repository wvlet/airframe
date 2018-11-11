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
import wvlet.airframe.surface

/**
  *
  */
class ScalaStandardCodecTest extends CodecSpec {
  "ScalaStandardCodec" should {
    "support Option[A]" in {
      val v = Some("hello")
      roundtrip(surface.of[Option[String]], Some("hello"))
      roundtrip[Option[String]](surface.of[Option[String]], None)
      roundtrip[Option[Int]](surface.of[Option[Int]], None)
      roundtrip[Option[Seq[Int]]](surface.of[Option[Seq[Int]]], Some(Seq(1, 2, 3)))
    }

    "support tuple" in {
      roundtrip[Tuple1[String]](surface.of[Tuple1[String]], Tuple1("hello"))
      roundtrip(surface.of[(Int, Int)], (1, 2))
      roundtrip(surface.of[(Int, Int, Int)], (1, 2, 3))
      roundtrip(surface.of[(Int, String, Boolean, Float)], (1, "a", true, 2.0f))
      roundtrip(surface.of[(Int, String, Boolean, Float, String)], (1, "a", true, 2.0f, "hello"))
      roundtrip(surface.of[(Int, String, Boolean, Float, String, Seq[Int])],
                (1, "a", true, 2.0f, "hello", Seq(1, 3, 4)))

      roundtrip(surface.of[(Int, Int, Int, Int, Int, Int, Int)], (1, 2, 3, 4, 5, 6, 7))
      roundtrip(surface.of[(Int, Int, Int, Int, Int, Int, Int, Int)], (1, 2, 3, 4, 5, 6, 7, 8))
      roundtrip(surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int)], (1, 2, 3, 4, 5, 6, 7, 8, 9))
      roundtrip(surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)], (1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
      roundtrip(surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
                (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
      roundtrip(surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
                (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
      roundtrip(surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
                (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13))
      roundtrip(surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
                (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14))
      roundtrip(surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
                (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15))
      roundtrip(surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
                (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16))
      roundtrip(surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
                (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16))
      roundtrip(surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
                (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
      roundtrip(
        surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
        (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18))
      roundtrip(
        surface
          .of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
        (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19))
      roundtrip(
        surface.of[
          (Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
        (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
      )
      roundtrip(
        surface
          .of[
            (Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int,
             Int)],
        (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21)
      )
    }
  }
}
