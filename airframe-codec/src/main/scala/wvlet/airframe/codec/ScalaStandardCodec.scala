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

import wvlet.airframe.msgpack.spi._

/**
  *
  */
object ScalaStandardCodec {
  case class OptionCodec[A](elementCodec: MessageCodec[A]) extends MessageCodec[Option[A]] {
    override def pack(p: Packer, v: Option[A]): Unit = {
      v match {
        case None => p.packNil
        case Some(x) =>
          elementCodec.pack(p, x)
      }
    }

    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      u.getNextValueType match {
        case ValueType.NIL =>
          v.setObject(None)
        case _ =>
          elementCodec.unpack(u, v)
          v.setObject(Some(v.getLastValue))
      }
    }
  }

  case class TupleCodec(elementCodec: Seq[MessageCodec[_]]) extends MessageCodec[Product] {

    override def pack(p: Packer, v: Product): Unit = {
      val arity = v.productArity
      p.packArrayHeader(arity)
      for ((e, codec) <- v.productIterator.toSeq.zip(elementCodec)) {
        codec.asInstanceOf[MessageCodec[Any]].pack(p, e)
      }
    }

    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      val numElems = u.unpackArrayHeader
      if (numElems != elementCodec.size) {
        u.skipValue(numElems)
        v.setIncompatibleFormatException(this, s"tuple size mismatch: expected ${elementCodec.size}, actual:${numElems}")
      } else {
        val b = Array.newBuilder[Any]
        for (codec <- elementCodec) {
          codec.unpack(u, v)
          b += v.getLastValue
        }
        val t = b.result()

        assert(t.length == numElems)

        val tuple = numElems match {
          case 1  => Tuple1(t(0))
          case 2  => (t(0), t(1))
          case 3  => (t(0), t(1), t(2))
          case 4  => (t(0), t(1), t(2), t(3))
          case 5  => (t(0), t(1), t(2), t(3), t(4))
          case 6  => (t(0), t(1), t(2), t(3), t(4), t(5))
          case 7  => (t(0), t(1), t(2), t(3), t(4), t(5), t(6))
          case 8  => (t(0), t(1), t(2), t(3), t(4), t(5), t(6), t(7))
          case 9  => (t(0), t(1), t(2), t(3), t(4), t(5), t(6), t(7), t(8))
          case 10 => (t(0), t(1), t(2), t(3), t(4), t(5), t(6), t(7), t(8), t(9))
          case 11 => (t(0), t(1), t(2), t(3), t(4), t(5), t(6), t(7), t(8), t(9), t(10))
          case 12 => (t(0), t(1), t(2), t(3), t(4), t(5), t(6), t(7), t(8), t(9), t(10), t(11))
          case 13 => (t(0), t(1), t(2), t(3), t(4), t(5), t(6), t(7), t(8), t(9), t(10), t(11), t(12))
          case 14 => (t(0), t(1), t(2), t(3), t(4), t(5), t(6), t(7), t(8), t(9), t(10), t(11), t(12), t(13))
          case 15 => (t(0), t(1), t(2), t(3), t(4), t(5), t(6), t(7), t(8), t(9), t(10), t(11), t(12), t(13), t(14))
          case 16 => (t(0), t(1), t(2), t(3), t(4), t(5), t(6), t(7), t(8), t(9), t(10), t(11), t(12), t(13), t(14), t(15))
          case 17 => (t(0), t(1), t(2), t(3), t(4), t(5), t(6), t(7), t(8), t(9), t(10), t(11), t(12), t(13), t(14), t(15), t(16))
          case 18 => (t(0), t(1), t(2), t(3), t(4), t(5), t(6), t(7), t(8), t(9), t(10), t(11), t(12), t(13), t(14), t(15), t(16), t(17))
          case 19 => (t(0), t(1), t(2), t(3), t(4), t(5), t(6), t(7), t(8), t(9), t(10), t(11), t(12), t(13), t(14), t(15), t(16), t(17), t(18))
          case 20 => (t(0), t(1), t(2), t(3), t(4), t(5), t(6), t(7), t(8), t(9), t(10), t(11), t(12), t(13), t(14), t(15), t(16), t(17), t(18), t(19))
          case 21 => (t(0), t(1), t(2), t(3), t(4), t(5), t(6), t(7), t(8), t(9), t(10), t(11), t(12), t(13), t(14), t(15), t(16), t(17), t(18), t(19), t(20))
          case _  => null
        }
        if (tuple != null) {
          v.setObject(tuple)
        } else {
          v.setIncompatibleFormatException(this, s"Tuples of ${numElems} elements is not supported")
        }
      }
    }
  }
}
