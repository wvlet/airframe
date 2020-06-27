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
package wvlet.airframe.msgpack.impl
import java.math.BigInteger

import org.msgpack.core.{MessageBufferPacker, MessagePacker}
import org.msgpack.{value => v8}
import wvlet.airframe.msgpack.io.ByteArrayBuffer
import wvlet.airframe.msgpack.spi._

/**
  * Adapter to msgpack-core's MessagePacker
  */
class PackerImpl(packer: MessagePacker) extends Packer {
  override def totalByteSize: Long = {
    packer.getTotalWrittenBytes
  }

  override def packNil: PackerImpl.this.type = {
    packer.packNil
    this
  }

  override def packBoolean(v: Boolean): PackerImpl.this.type = {
    packer.packBoolean(v)
    this
  }
  override def packByte(v: Byte): PackerImpl.this.type = {
    packer.packByte(v)
    this
  }
  override def packShort(v: Short): PackerImpl.this.type = {
    packer.packShort(v)
    this
  }
  override def packInt(v: Int): PackerImpl.this.type = {
    packer.packInt(v)
    this
  }
  override def packLong(v: Long): PackerImpl.this.type = {
    packer.packLong(v)
    this
  }
  override def packBigInteger(v: BigInteger): PackerImpl.this.type = {
    packer.packBigInteger(v)
    this
  }
  override def packFloat(v: Float): PackerImpl.this.type = {
    packer.packFloat(v)
    this
  }
  override def packDouble(v: Double): PackerImpl.this.type = {
    packer.packDouble(v)
    this
  }
  override def packString(v: String): PackerImpl.this.type = {
    packer.packString(v)
    this
  }
  override def packTimestamp(epochSecond: Long, nanoAdjustment: Int = 0): PackerImpl.this.type = {
    val extData = PackerImpl.timeStampExtBytes(epochSecond, nanoAdjustment)
    packer.writePayload(extData)
    this
  }

  override def packArrayHeader(arraySize: Int): PackerImpl.this.type = {
    packer.packArrayHeader(arraySize)
    this
  }

  override def packMapHeader(mapSize: Int): PackerImpl.this.type = {
    packer.packMapHeader(mapSize)
    this
  }

  override def packExtensionTypeHeader(extType: Byte, payloadLen: Int): PackerImpl.this.type = {
    packer.packExtensionTypeHeader(extType, payloadLen)
    this
  }

  override def packBinaryHeader(len: Int): PackerImpl.this.type = {
    packer.packBinaryHeader(len)
    this
  }

  override def packRawStringHeader(len: Int): PackerImpl.this.type = {
    packer.packRawStringHeader(len)
    this
  }

  override def packValue(v: Value): PackerImpl.this.type = {
    packer.packValue(PackerImpl.toMsgPackV8Value(v))
    this
  }

  override def writePayload(src: Array[Byte]): PackerImpl.this.type = {
    packer.writePayload(src)
    this
  }

  override def writePayload(src: Array[Byte], offset: Int, length: Int): PackerImpl.this.type = {
    packer.writePayload(src, offset, length)
    this
  }
  override def addPayload(src: Array[Byte]): PackerImpl.this.type = {
    packer.addPayload(src)
    this
  }

  override def addPayload(src: Array[Byte], offset: Int, length: Int): PackerImpl.this.type = {
    packer.addPayload(src, offset, length)
    this
  }
  override def close(): Unit = {
    packer.close()
  }
}

class BufferPackerImpl(bufferPacker: MessageBufferPacker) extends PackerImpl(bufferPacker) with BufferPacker {
  override def toByteArray: Array[Byte] = bufferPacker.toByteArray
  override def clear: Unit              = bufferPacker.clear()
}

object PackerImpl {
  def timeStampExtBytes(epochSecond: Long, nanoAdjustment: Int): Array[Byte] = {
    val buf    = ByteArrayBuffer.newBuffer(15)
    val cursor = WriteCursor(buf, 0)
    OffsetPacker.packTimestampEpochSecond(cursor, epochSecond, nanoAdjustment)
    val extData = buf.readBytes(0, cursor.lastWrittenBytes)
    extData
  }

  def toMsgPackV8Value(v: Value): v8.Value = {
    import wvlet.airframe.msgpack.spi.Value._
    v match {
      case NilValue                   => v8.ValueFactory.newNil()
      case BooleanValue(v)            => v8.ValueFactory.newBoolean(v)
      case LongValue(v)               => v8.ValueFactory.newInteger(v)
      case BigIntegerValue(v)         => v8.ValueFactory.newInteger(v)
      case DoubleValue(v)             => v8.ValueFactory.newFloat(v)
      case StringValue(v)             => v8.ValueFactory.newString(v)
      case BinaryValue(v)             => v8.ValueFactory.newBinary(v)
      case ExtensionValue(extType, v) => v8.ValueFactory.newExtension(extType, v)
      case TimestampValue(v) => {
        val extBytes = timeStampExtBytes(v.getEpochSecond, v.getNano)
        v8.ValueFactory.newExtension(-1, extBytes)
      }
      case ArrayValue(elems) =>
        import scala.jdk.CollectionConverters._
        val values = elems.map(x => toMsgPackV8Value(x)).toList.asJava
        v8.ValueFactory.newArray(values)
      case MapValue(entries) =>
        val b = v8.ValueFactory.newMapBuilder()
        entries.map { case (k, v) => b.put(toMsgPackV8Value(k), toMsgPackV8Value(v)) }
        b.build()
    }
  }
}
