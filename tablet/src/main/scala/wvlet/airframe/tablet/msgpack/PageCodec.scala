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

import org.msgpack.core.{MessagePack, MessagePacker, MessageUnpacker}
import wvlet.airframe.tablet.Schema.DataType
import wvlet.airframe.tablet.msgpack.CollectionCodec.SeqCodec
import wvlet.airframe.tablet.msgpack.PageCodec._
import wvlet.airframe.tablet.{Page, Schema}
import wvlet.surface.Surface

case class PageCodec[A](surface: Surface, elementCodec: MessageCodec[A]) extends MessageCodec[Page[A]] {
  override def pack(p: MessagePacker, v: Page[A]): Unit = {
    // Encode page data
    // TODO reuse buffer
    val pagePacker = MessagePack.newDefaultBufferPacker()
    schemaCodec.pack(pagePacker, v.schema)
    SeqCodec(elementCodec).pack(pagePacker, v.data)

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
      v.setIncompatibleFormatException(this, s"Unsupported ext type ${extType}. Expected = ${PAGE_CODEC_ID}")
    } else {
      val pageUnpacker = MessagePack.newDefaultUnpacker(data.sliceAsByteBuffer())
      schemaCodec.unpack(pageUnpacker, v)
      if (v.isNull) {
        v.setIncompatibleFormatException(this, s"Failed to read schema")
      } else {
        val schema = v.getLastValue.asInstanceOf[Schema]
        // Read data
        SeqCodec(elementCodec).unpack(pageUnpacker, v)

        if (v.isNull) {
          v.setIncompatibleFormatException(this, s"Failed to read page content")
        } else {
          v.setObject(Page(schema, v.getLastValue.asInstanceOf[Seq[_]]))
        }
      }
    }
  }
}

object ColumnTypeCodec extends MessageCodec[DataType] {
  override def pack(p: MessagePacker, v: DataType): Unit = {
    p.packString(v.signature)
  }
  override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
    val columnType = u.unpackString()
    DataType.unapply(columnType) match {
      case Some(c) => v.setObject(c)
      case None    => v.setIncompatibleFormatException(this, s"Invalid ColumnType value: ${columnType}")
    }
  }
}

/**
  *
  */
object PageCodec {
  val schemaCodec         = MessageCodec.of[Schema]
  val PAGE_CODEC_ID: Byte = 0
}
