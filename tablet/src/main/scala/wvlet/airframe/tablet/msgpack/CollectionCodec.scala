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

import java.util

import org.msgpack.core.{MessagePack, MessagePacker, MessageUnpacker}
import org.msgpack.value.StringValue
import wvlet.airframe.tablet.{Page, Schema}
import wvlet.surface.Surface
import org.msgpack.value.ValueFactory._
import wvlet.airframe.tablet.Schema.ColumnType

import scala.collection.JavaConverters._

/**
  *
  */
object CollectionCodec {

  implicit class RichString(s: String) {
    def toValue: StringValue = newString(s)
  }

  val schemaCodec = MessageCodec.of[Schema]

  object ColumnTypeCodec extends MessageCodec[ColumnType] {
    override def pack(p: MessagePacker, v: ColumnType): Unit = {
      p.packString(v.name)
    }
    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      val columnType = u.unpackString()
      ColumnType.unapply(columnType) match {
        case Some(c) => v.setObject(c)
        case None    => v.setIncompatibleFormatException(s"Invalid ColumnType value: ${columnType}")
      }
    }
  }

  val PAGE_CODEC_ID: Byte = 0

  case class PageCodec[A](surface: Surface, elementCodec: MessageCodec[A]) extends MessageCodec[Page[A]] {
    override def pack(p: MessagePacker, v: Page[A]): Unit = {
      // Encode page data
      // TODO reuse buffer
      val pagePacker = MessagePack.newDefaultBufferPacker()
      schemaCodec.pack(pagePacker, v.schema)
      SeqCodec(surface, elementCodec).pack(pagePacker, v.data)

      // Encode page using ext type
      val pageContent = pagePacker.toByteArray
      p.packExtensionTypeHeader(PAGE_CODEC_ID, pageContent.length)
      p.addPayload(pageContent)
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      val extHeader = u.unpackExtensionTypeHeader()
      val extType   = extHeader.getType

      val data = u.readPayloadAsReference(extHeader.getLength)
      if (extType != PAGE_CODEC_ID) {
        v.setIncompatibleFormatException(s"Unsuppoted ext type ${extType}. Expected = ${PAGE_CODEC_ID}")
      } else {
        val pageUnpacker = MessagePack.newDefaultUnpacker(data.sliceAsByteBuffer())
        schemaCodec.unpack(pageUnpacker, v)
        if (v.isNull) {
          v.setIncompatibleFormatException(s"Failed to read schema")
        } else {
          val schema = v.getLastValue.asInstanceOf[Schema]
          // Read data
          SeqCodec(surface, elementCodec).unpack(pageUnpacker, v)

          if (v.isNull) {
            v.setIncompatibleFormatException(s"Failed to read page content")
          } else {
            v.setObject(Page(schema, v.getLastValue.asInstanceOf[Seq[_]]))
          }
        }
      }
    }
  }

  case class SeqCodec[A](surface: Surface, elementCodec: MessageCodec[A]) extends MessageCodec[Seq[A]] {
    override def pack(p: MessagePacker, v: Seq[A]): Unit = {
      // elements
      // [e1, e2, ...]
      val len = v.length
      p.packArrayHeader(len)
      for (e <- v) {
        elementCodec.pack(p, e)
      }
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      // Read elements
      val len = u.unpackArrayHeader()
      val b   = Seq.newBuilder[Any]
      b.sizeHint(len)
      for (i <- 0 until len) {
        elementCodec.unpack(u, v)
        if (!v.isNull) {
          b += v.getLastValue
        } else {}
      }
      v.setObject(b.result())
    }
  }

  // TODO Just use SeqCodec for Scala and adapt the result type
  case class JavaListCodec[A](elementCodec: MessageCodec[A]) extends MessageCodec[java.util.List[A]] {
    override def pack(p: MessagePacker, v: util.List[A]): Unit = {
      val len = v.size
      p.packArrayHeader(len)
      for (e <- v.asScala) {
        elementCodec.pack(p, e)
      }
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      val len = u.unpackArrayHeader()
      val b   = Seq.newBuilder[Any]
      b.sizeHint(len)
      for (i <- 0 until len) {
        elementCodec.unpack(u, v)
        b += v.getLastValue
      }
      v.setObject(b.result().asJava)
    }
  }

  case class MapCodec[A, B](keyCodec: MessageCodec[A], valueCodec: MessageCodec[B]) extends MessageCodec[Map[A, B]] {
    override def pack(p: MessagePacker, m: Map[A, B]): Unit = {
      p.packMapHeader(m.size)
      for ((k, v) <- m.seq) {
        keyCodec.pack(p, k)
        valueCodec.pack(p, v)
      }
    }
    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      val len = u.unpackMapHeader()
      val b   = Map.newBuilder[Any, Any]
      b.sizeHint(len)
      for (i <- 0 until len) {
        keyCodec.unpack(u, v)
        val key = v.getLastValue
        valueCodec.unpack(u, v)
        val value = v.getLastValue
        b += (key -> value)
      }
      v.setObject(b.result())
    }
  }

  // TODO Just use MapCodec for Scala and adapt the result type
  case class JavaMapCodec[A, B](keyCodec: MessageCodec[A], valueCodec: MessageCodec[B]) extends MessageCodec[java.util.Map[A, B]] {
    override def pack(p: MessagePacker, m: java.util.Map[A, B]): Unit = {
      p.packMapHeader(m.size)
      for ((k, v) <- m.asScala.seq) {
        keyCodec.pack(p, k)
        valueCodec.pack(p, v)
      }
    }
    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      val len = u.unpackMapHeader()
      val b   = Map.newBuilder[Any, Any]
      b.sizeHint(len)
      for (i <- 0 until len) {
        keyCodec.unpack(u, v)
        val key = v.getLastValue
        valueCodec.unpack(u, v)
        val value = v.getLastValue
        b += (key -> value)
      }
      v.setObject(b.result().asJava)
    }
  }

}
