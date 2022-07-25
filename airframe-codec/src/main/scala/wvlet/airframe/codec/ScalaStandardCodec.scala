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

import wvlet.airframe.msgpack.spi.Value.NilValue
import wvlet.airframe.msgpack.spi.{MessagePack, Packer, Unpacker, ValueType}

/**
  */
object ScalaStandardCodec {
  case class OptionCodec[A](elementCodec: MessageCodec[A]) extends MessageCodec[Option[A]] {
    override def pack(p: Packer, v: Option[A]): Unit = {
      v match {
        case null | None =>
          p.packNil
        case Some(x) =>
          elementCodec.pack(p, x)
      }
    }

    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      val f = u.getNextFormat
      f.getValueType match {
        case ValueType.NIL =>
          u.unpackNil
          v.setObject(None)
        case _ =>
          elementCodec.unpack(u, v)
          Option(v.getLastValue) match {
            case Some(x) =>
              v.setObject(Some(x))
            case None =>
              v.setNull
          }
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

    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      val numElems = u.unpackArrayHeader
      if (numElems != elementCodec.size) {
        u.skipValue(numElems)
        v.setIncompatibleFormatException(
          this,
          s"tuple size mismatch: expected ${elementCodec.size}, actual:${numElems}"
        )
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
          case 16 =>
            (t(0), t(1), t(2), t(3), t(4), t(5), t(6), t(7), t(8), t(9), t(10), t(11), t(12), t(13), t(14), t(15))
          case 17 =>
            (
              t(0),
              t(1),
              t(2),
              t(3),
              t(4),
              t(5),
              t(6),
              t(7),
              t(8),
              t(9),
              t(10),
              t(11),
              t(12),
              t(13),
              t(14),
              t(15),
              t(16)
            )
          case 18 =>
            (
              t(0),
              t(1),
              t(2),
              t(3),
              t(4),
              t(5),
              t(6),
              t(7),
              t(8),
              t(9),
              t(10),
              t(11),
              t(12),
              t(13),
              t(14),
              t(15),
              t(16),
              t(17)
            )
          case 19 =>
            (
              t(0),
              t(1),
              t(2),
              t(3),
              t(4),
              t(5),
              t(6),
              t(7),
              t(8),
              t(9),
              t(10),
              t(11),
              t(12),
              t(13),
              t(14),
              t(15),
              t(16),
              t(17),
              t(18)
            )
          case 20 =>
            (
              t(0),
              t(1),
              t(2),
              t(3),
              t(4),
              t(5),
              t(6),
              t(7),
              t(8),
              t(9),
              t(10),
              t(11),
              t(12),
              t(13),
              t(14),
              t(15),
              t(16),
              t(17),
              t(18),
              t(19)
            )
          case 21 =>
            (
              t(0),
              t(1),
              t(2),
              t(3),
              t(4),
              t(5),
              t(6),
              t(7),
              t(8),
              t(9),
              t(10),
              t(11),
              t(12),
              t(13),
              t(14),
              t(15),
              t(16),
              t(17),
              t(18),
              t(19),
              t(20)
            )
          case _ => null
        }
        if (tuple != null) {
          v.setObject(tuple)
        } else {
          v.setIncompatibleFormatException(this, s"Tuples of ${numElems} elements is not supported")
        }
      }
    }
  }

  case class EitherCodec[A, B](leftCodec: MessageCodec[A], rightCodec: MessageCodec[B])
      extends MessageCodec[Either[A, B]] {
    override def pack(p: Packer, v: Either[A, B]): Unit = {
      // Encode Either[A, B] as Array[2](left, nil) or Array[2](nil, right)
      p.packArrayHeader(2)
      v match {
        case Left(l) =>
          leftCodec.pack(p, l)
          p.packNil
        case Right(r) =>
          p.packNil
          rightCodec.pack(p, r)
      }
    }

    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      u.getNextValueType match {
        case ValueType.ARRAY =>
          val size = u.unpackArrayHeader
          if (size != 2) {
            u.skipValue(size)
            v.setIncompatibleFormatException(
              this,
              s"EitherCodec ${this} expects Array[2] input value, but get Array[${size}]"
            )
          } else {
            // Parse Array[2]
            val left  = u.unpackValue
            val right = u.unpackValue
            (left, right) match {
              case (l, NilValue) =>
                leftCodec.unpack(MessagePack.newUnpacker(l.toMsgpack), v)
                if (!v.isNull) {
                  v.setObject(Left(v.getLastValue))
                }
              case (NilValue, r) =>
                rightCodec.unpack(MessagePack.newUnpacker(r.toMsgpack), v)
                if (!v.isNull) {
                  v.setObject(Right(v.getLastValue))
                }
              case _ =>
                v.setIncompatibleFormatException(
                  this,
                  s"Unexpected input for Either ${this}: Array(${left}, ${right})"
                )
            }
          }
        case other =>
          u.skipValue
          v.setIncompatibleFormatException(
            this,
            s"EitherCodec ${this} can read only Array[2] MessagePack value, but found ${other}"
          )
      }
    }
  }
}
