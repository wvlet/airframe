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

import org.scalacheck.Arbitrary
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import wvlet.airframe.surface.{ArraySurface, GenericSurface, Surface}
import wvlet.airframe.{AirframeSpec, msgpack}

import scala.collection.JavaConverters._

/**
  *
  */
trait CodecSpec extends AirframeSpec with GeneratorDrivenPropertyChecks {
  def roundtrip[A](surface: Surface, v: A, expectedType: DataType = DataType.ANY): MessageHolder = {
    roundtrip[A](MessageCodec.ofSurface(surface).asInstanceOf[MessageCodec[A]], v, expectedType)
  }

  def roundtrip[A](codec: MessageCodec[A], v: A, expectedType: DataType): MessageHolder = {
    val h = new MessageHolder
    trace(s"Testing roundtrip of ${v} with ${codec}")
    val packer = msgpack.newBufferPacker
    codec.pack(packer, v)
    val unpacker = msgpack.newUnpacker(packer.toByteArray)
    codec.unpack(unpacker, h)

    h.isNull shouldBe false
    h.hasError shouldBe false
    h.getDataType shouldBe expectedType
    h.getLastValue shouldBe v
    h
  }

  def roundtripStr[A](codec: MessageCodec[A], v: A, expectedType: DataType): MessageHolder = {
    val h = new MessageHolder
    trace(s"Testing str based roundtrip of ${v} with ${codec}")
    val packer = msgpack.newBufferPacker
    packer.packString(v.toString)
    val unpacker = msgpack.newUnpacker(packer.toByteArray)
    codec.unpack(unpacker, h)

    h.isNull shouldBe false
    h.hasError shouldBe false
    h.getDataType shouldBe expectedType
    h.getLastValue shouldBe v
    h
  }

  def checkCodec[A](codec: MessageCodec[A], v: A): Unit = {
    val b = codec.toMsgPack(v)
    val r = codec.unpackBytes(b)
    r shouldBe defined
    v shouldBe r.get
  }

  def roundTripTest[T](surface: Surface, dataType: DataType)(implicit impArb: Arbitrary[T]): Unit = {
    forAll { (v: T) =>
      roundtrip(surface, v, dataType)
    }
  }

  def arrayRoundTripTest[T](surface: Surface)(implicit impArb: Arbitrary[Array[T]]): Unit = {
    val codec = MessageCodec.ofSurface(ArraySurface(surface.rawType, surface)).asInstanceOf[MessageCodec[Array[T]]]
    val seqCodec =
      MessageCodec.ofSurface(new GenericSurface(classOf[Seq[_]], Seq(surface))).asInstanceOf[MessageCodec[Seq[T]]]
    val javaListCodec = MessageCodec
      .ofSurface(new GenericSurface(classOf[java.util.List[_]], Seq(surface))).asInstanceOf[MessageCodec[
        java.util.List[T]]]
    forAll { (v: Array[T]) =>
      // Array round trip
      roundtrip(codec, v, DataType.ANY)
      // Seq -> Array
      roundtrip(seqCodec, v.toSeq, DataType.ANY)
      // java.util.List[T] -> Array
      roundtrip(javaListCodec, v.toSeq.asJava, DataType.ANY)
    }
  }

  def roundTripTestWithStr[T](surface: Surface, dataType: DataType)(implicit impArb: Arbitrary[T]): Unit = {
    val codec = MessageCodec.ofSurface(surface).asInstanceOf[MessageCodec[T]]
    forAll { (v: T) =>
      // Test input:T -> output:T
      roundtrip(codec, v, dataType)
      // Test from input:String -> output:T
      roundtripStr(codec, v, dataType)
    }
  }

}
