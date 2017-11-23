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
package wvlet.airframe.tablet.msgpack

/**
  *
  */
class ScalaStandardCodecTest extends CodecSpec {
  "ScalaStandardCodec" should {
    "support Option[A]" in {
      val v = Some("hello")
      roundtrip(Some("hello"))
      roundtrip[Option[String]](None)
      roundtrip[Option[Int]](None)
      roundtrip[Option[Seq[Int]]](Some(Seq(1, 2, 3)))
    }

    "support tuple" in {
      roundtrip[Tuple1[String]](Tuple1("hello"))
      roundtrip((1, 2))
      roundtrip((1, 2, 3))
      roundtrip((1, "a", true, 2.0f))
      roundtrip((1, "a", true, 2.0f, "hello"))
      roundtrip((1, "a", true, 2.0f, "hello", Seq(1, 3, 4)))

      roundtrip((1, 2, 3, 4, 5, 6, 7))
      roundtrip((1, 2, 3, 4, 5, 6, 7, 8))
      roundtrip((1, 2, 3, 4, 5, 6, 7, 8, 9))
      roundtrip((1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
      roundtrip((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
      roundtrip((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
      roundtrip((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13))
      roundtrip((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14))
      roundtrip((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15))
      roundtrip((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16))
      roundtrip((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16))
      roundtrip((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
      roundtrip((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18))
      roundtrip((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19))
      roundtrip((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20))
      roundtrip((1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21))
    }
  }
}
