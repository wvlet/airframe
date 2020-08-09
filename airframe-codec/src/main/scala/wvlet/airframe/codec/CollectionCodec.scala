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

import java.util

import wvlet.airframe.json.JSONParseException
import wvlet.airframe.msgpack.spi.{MessagePack, Packer, Unpacker, ValueType}
import wvlet.airframe.surface.{Surface, Zero}

import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.language.higherKinds
import scala.util.{Failure, Success, Try}

object CollectionCodec {
  object BaseSeqCodec {
    def pack[A](p: Packer, v: Seq[A], elementCodec: MessageCodec[A]): Unit = {
      // elements
      // [e1, e2, ...]
      val len = v.length
      p.packArrayHeader(len)
      for (e <- v) {
        elementCodec.pack(p, e)
      }
    }

    def unpack[A](
        u: Unpacker,
        v: MessageContext,
        surface: Surface,
        elementCodec: MessageCodec[A],
        newBuilder: => mutable.Builder[A, Seq[A]]
    ): Unit = {

      def unpackSingle(readElement: => A): Unit = {
        // Create a single element Seq.
        val b = newBuilder
        b.sizeHint(1)
        Try(readElement) match {
          case Success(x)  => b += x
          case Failure(ex) => b += Zero.zeroOf(surface).asInstanceOf[A]
        }
        v.setObject(b.result())
      }

      u.getNextValueType match {
        case ValueType.ARRAY =>
          // Read elements
          val len = u.unpackArrayHeader
          val b   = newBuilder
          b.sizeHint(len)
          for (i <- 0 until len) {
            elementCodec.unpack(u, v)
            if (v.isNull) {
              // Add default value
              val z = Zero.zeroOf(surface).asInstanceOf[A]
              b += z
            } else {
              b += v.getLastValue.asInstanceOf[A]
            }
          }
          v.setObject(b.result())
        case ValueType.STRING =>
          // Assumes JSON array
          val s = u.unpackString
          if (s.startsWith("[")) {
            try {
              val jsonArrayMsgPack = JSONCodec.toMsgPack(s)
              val unpacker         = MessagePack.newUnpacker(jsonArrayMsgPack)
              // Parse again
              unpack(unpacker, v, surface, elementCodec, newBuilder)
            } catch {
              case e: JSONParseException =>
                // Not a JSON value, so create a single element Seq
                unpackSingle(elementCodec.fromString(s))
            }
          } else {
            // Create a single element Seq
            unpackSingle(elementCodec.fromString(s))
          }
        case ValueType.NIL =>
          u.unpackNil
          v.setObject(newBuilder.result())
        case ValueType.BOOLEAN | ValueType.INTEGER | ValueType.FLOAT | ValueType.BINARY =>
          val x = u.unpackValue
          unpackSingle(elementCodec.fromMsgPack(x.toMsgpack))
        case other =>
          u.skipValue
          v.setError(
            new MessageCodecException(INVALID_DATA, elementCodec, s"Unsupported type for Seq[$surface]: ${other}")
          )
      }
    }
  }

  class SeqCodec[A](surface: Surface, elementCodec: MessageCodec[A]) extends MessageCodec[Seq[A]] {
    override def pack(p: Packer, v: Seq[A]): Unit = {
      BaseSeqCodec.pack(p, v, elementCodec)
    }

    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      BaseSeqCodec.unpack(u, v, surface, elementCodec, Seq.newBuilder[A])
    }
  }

  class IndexedSeqCodec[A](surface: Surface, elementCodec: MessageCodec[A]) extends MessageCodec[IndexedSeq[A]] {
    override def pack(p: Packer, v: IndexedSeq[A]): Unit = {
      BaseSeqCodec.pack(p, v, elementCodec)
    }

    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      BaseSeqCodec.unpack(u, v, surface, elementCodec, IndexedSeq.newBuilder[A])
    }
  }

  /*
    Note: If we use MessageCodec[List[A]] it causes StackOverflow. Need more investigation
    <code>
    [error] java.lang.StackOverflowError
    [error] 	at scala.reflect.internal.tpe.TypeMaps$SubstMap.apply(TypeMaps.scala:764)
    [error] 	at scala.reflect.internal.tpe.TypeMaps$SubstSymMap.apply(TypeMaps.scala:825)
    [error] 	at scala.reflect.internal.tpe.TypeMaps$TypeMap.mapOver(TypeMaps.scala:172)
    </code>
   */
  class ListCodec[A](surface: Surface, elementCodec: MessageCodec[A]) extends MessageCodec[Seq[A]] {
    override def pack(p: Packer, v: Seq[A]): Unit = {
      BaseSeqCodec.pack(p, v, elementCodec)
    }

    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      BaseSeqCodec.unpack(u, v, surface, elementCodec, List.newBuilder[A])
    }
  }

  // TODO Just use SeqCodec for Scala and adapt the result type
  case class JavaListCodec[A](elementCodec: MessageCodec[A]) extends MessageCodec[java.util.List[A]] {
    override def pack(p: Packer, v: util.List[A]): Unit = {
      val len = v.size
      p.packArrayHeader(len)
      for (e <- v.asScala) {
        elementCodec.pack(p, e)
      }
    }

    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      val len = u.unpackArrayHeader
      val b   = Seq.newBuilder[Any]
      b.sizeHint(len)
      for (i <- 0 until len) {
        elementCodec.unpack(u, v)
        b += v.getLastValue
      }
      v.setObject(b.result().asJava)
    }
  }

  abstract class MapCodecBase[A, B, MapType[A, B]](keyCodec: MessageCodec[A], valueCodec: MessageCodec[B])
      extends MessageCodec[MapType[A, B]] {
    protected def packMap(p: Packer, m: Map[A, B]): Unit = {
      p.packMapHeader(m.size)
      for ((k, v) <- m) {
        keyCodec.pack(p, k)
        valueCodec.pack(p, v)
      }
    }

    protected def newMapBuilder: mutable.Builder[(Any, Any), Map[Any, Any]]
    protected def castResult(v: Any): MapType[A, B]

    protected def unpackMap(u: Unpacker, v: MessageContext): Unit = {
      u.getNextFormat.getValueType match {
        case ValueType.MAP =>
          val len = u.unpackMapHeader
          val b   = newMapBuilder
          b.sizeHint(len)
          for (i <- 0 until len) {
            keyCodec.unpack(u, v)
            val key = v.getLastValue
            valueCodec.unpack(u, v)
            val value = v.getLastValue
            b += (key -> value)
          }
          v.setObject(castResult(b.result()))
        case ValueType.STRING =>
          // Assume it's a JSON map value
          val json = u.unpackString
          try {
            val msgpack = JSONCodec.toMsgPack(json)
            this.unpackMsgPack(msgpack).map { x => v.setObject(x) }
          } catch {
            case e: JSONParseException =>
              v.setError(e)
          }
        case _ =>
          val x = u.unpackValue
          v.setError(new MessageCodecException(INVALID_DATA, this, s"Not a map value: ${x}"))
      }
    }
  }

  case class MapCodec[A, B](keyCodec: MessageCodec[A], valueCodec: MessageCodec[B])
      extends MapCodecBase[A, B, Map](keyCodec, valueCodec) {
    override protected def newMapBuilder: mutable.Builder[(Any, Any), Map[Any, Any]] = Map.newBuilder[Any, Any]
    override protected def castResult(v: Any): Map[A, B]                             = v.asInstanceOf[Map[A, B]]
    override def pack(p: Packer, m: Map[A, B]): Unit                                 = packMap(p, m)
    override def unpack(u: Unpacker, v: MessageContext): Unit                        = unpackMap(u, v)
  }

  case class ListMapCodec[A, B](keyCodec: MessageCodec[A], valueCodec: MessageCodec[B])
      extends MapCodecBase[A, B, ListMap](keyCodec, valueCodec) {
    override protected def newMapBuilder: mutable.Builder[(Any, Any), Map[Any, Any]] = ListMap.newBuilder[Any, Any]
    override protected def castResult(v: Any): ListMap[A, B]                         = v.asInstanceOf[ListMap[A, B]]
    override def pack(p: Packer, m: ListMap[A, B]): Unit                             = packMap(p, m)
    override def unpack(u: Unpacker, v: MessageContext): Unit                        = unpackMap(u, v)
  }

  // TODO Just use MapCodec for Scala and adapt the result type
  case class JavaMapCodec[A, B](keyCodec: MessageCodec[A], valueCodec: MessageCodec[B])
      extends MapCodecBase[A, B, java.util.Map](keyCodec, valueCodec) {

    override protected def newMapBuilder: mutable.Builder[(Any, Any), Map[Any, Any]] = Map.newBuilder[Any, Any]
    override protected def castResult(v: Any): util.Map[A, B]                        = v.asInstanceOf[Map[A, B]].asJava

    override def pack(p: Packer, m: util.Map[A, B]): Unit     = packMap(p, m.asScala.toMap)
    override def unpack(u: Unpacker, v: MessageContext): Unit = unpackMap(u, v)
  }

}
