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

import java.io.File

import org.msgpack.core.{MessagePack, MessagePacker}
import wvlet.airframe.AirframeSpec
import wvlet.airframe.tablet.Schema
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import wvlet.airframe.tablet.Schema.DataType
import MessageCodecFactoryTest._
import org.scalacheck.Arbitrary

import scala.reflect.runtime.{universe => ru}

/**
  *
  */
class MessageCodecFactoryTest extends AirframeSpec {

  def roundtrip[A](codec: MessageCodec[A], v: A, expectedType: DataType): MessageHolder = {
    val h = new MessageHolder
    trace(s"Testing roundtrip of ${v} with ${codec}")
    val packer = MessagePack.newDefaultBufferPacker()
    codec.pack(packer, v)
    val unpacker = MessagePack.newDefaultUnpacker(packer.toByteArray)
    codec.unpack(unpacker, h)

    h.isNull shouldBe false
    h.getDataType shouldBe expectedType
    h.getLastValue shouldBe v
    h
  }

  def roundtripStr[A](codec: MessageCodec[A], v: A, expectedType: DataType): MessageHolder = {
    val h = new MessageHolder
    trace(s"Testing str based roundtrip of ${v} with ${codec}")
    val packer = MessagePack.newDefaultBufferPacker()
    packer.packString(v.toString)
    val unpacker = MessagePack.newDefaultUnpacker(packer.toByteArray)
    codec.unpack(unpacker, h)

    h.isNull shouldBe false
    h.getDataType shouldBe expectedType
    h.getLastValue shouldBe v
    h
  }

  def checkCodec[A](codec: MessageCodec[A], v: A) {
    val b = codec.pack(v)
    val r = codec.unpack(b)
    r shouldBe defined
    v shouldBe r.get
  }

  def roundTripTest[T: ru.TypeTag](dataType: DataType)(implicit impArb: Arbitrary[T]) {
    val codec = MessageCodec.of[T]
    forAll { (v: T) =>
      roundtrip(codec, v, dataType)
    }
  }

  def arrayRoundTripTest[T: ru.TypeTag](implicit impArb: Arbitrary[Array[T]]) {
    val codec    = MessageCodec.of[Array[T]]
    val seqCodec = MessageCodec.of[Seq[T]]
    forAll { (v: Array[T]) =>
      roundtrip(codec, v, Schema.ANY)
      roundtrip(seqCodec, v.toSeq, Schema.ANY)
    }
  }

  def roundTripTestWithStr[T: ru.TypeTag](dataType: DataType)(implicit impArb: Arbitrary[T]) {
    val codec = MessageCodec.of[T]
    forAll { (v: T) =>
      // Test input:T -> output:T
      roundtrip(codec, v, dataType)
      // Test from input:String -> output:T
      roundtripStr(codec, v, dataType)
    }
  }

  "MessageCodecFactory" should {
    "support numeric" in {
      roundTripTestWithStr[Int](Schema.INTEGER)
      roundTripTestWithStr[Byte](Schema.INTEGER)
      roundTripTestWithStr[Short](Schema.INTEGER)
      roundTripTestWithStr[Long](Schema.INTEGER)
      roundTripTestWithStr[Boolean](Schema.BOOLEAN)
    }

    "support char" in {
      roundTripTest[Char](Schema.INTEGER)
    }

    "support float" in {
      roundTripTestWithStr[Float](Schema.FLOAT)
      roundTripTestWithStr[Double](Schema.FLOAT)
    }

    "support string" in {
      roundTripTest[String](Schema.STRING)
    }

    "support arrays" taggedAs ("array") in {
      arrayRoundTripTest[Byte]
      arrayRoundTripTest[Char]
      arrayRoundTripTest[Int]
      arrayRoundTripTest[Short]
      arrayRoundTripTest[Long]
      arrayRoundTripTest[String]
      arrayRoundTripTest[Float]
      arrayRoundTripTest[Double]
      arrayRoundTripTest[Boolean]
    }

    "support File" in {
      val codec          = MessageCodec.of[File]
      def check(v: File) = checkCodec(codec, v)
      check(new File("sample.txt"))
      check(new File("/var/log"))
      check(new File("/etc/conf.d/myconf.conf"))
      check(new File("c:/etc/conf.d/myconf.conf"))
      check(new File("."))
      check(new File(".."))
      check(new File("relative/path.txt"))
    }

    "support case classes" in {
      val codec = MessageCodec.of[A1]
      val v: A1 = A1(1, 2, 3, 4, 5, 6, true, "str")
      roundtrip(codec, v, Schema.ANY)

      forAll { (i: Int, l: Long, f: Float, d: Double, c: Char, st: Short) =>
        // scalacheck supports only upto 6 elements
        forAll { (b: Boolean, s: String) =>
          val v = A1(i, l, f, d, c, st, b, s)
          roundtrip[A1](codec, v, Schema.ANY)
        }
      }
    }
  }
}

object MessageCodecFactoryTest {

  case class A1(
      i: Int,
      l: Long,
      f: Float,
      d: Double,
      c: Char,
      st: Short,
      b: Boolean,
      s: String
  )

}
