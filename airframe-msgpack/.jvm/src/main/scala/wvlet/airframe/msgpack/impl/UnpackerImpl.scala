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
import java.time.Instant

import org.msgpack.core.{MessageTypeException, MessageUnpacker}
import wvlet.airframe.msgpack.io.ByteArrayBuffer
import wvlet.airframe.msgpack.spi._

/**
  *
  */
class UnpackerImpl(unpacker: MessageUnpacker) extends Unpacker {
  override def hasNext: Boolean = {
    unpacker.hasNext
  }
  override def getNextFormat: MessageFormat = {
    val mf = unpacker.getNextFormat
    UnpackerImpl.conversionTable(mf)
  }
  override def getNextValueType: ValueType = {
    getNextFormat.valueType
  }

  override def skipValue: Unit = {
    unpacker.skipValue()
  }
  override def skipValue(count: Int): Unit = {
    unpacker.skipValue(count)
  }

  override def unpackNil: Unit = {
    unpacker.unpackNil()
  }
  override def tryUnpackNil: Boolean = {
    unpacker.tryUnpackNil()
  }
  override def unpackBoolean: Boolean = {
    unpacker.unpackBoolean()
  }
  override def unpackByte: Byte = {
    unpacker.unpackByte()
  }
  override def unpackShort: Short = {
    unpacker.unpackShort()
  }
  override def unpackInt: Int = {
    unpacker.unpackInt()
  }
  override def unpackLong: Long = {
    unpacker.unpackLong()
  }
  override def unpackBigInteger: BigInteger = {
    unpacker.unpackBigInteger()
  }
  override def unpackFloat: Float = {
    unpacker.unpackFloat()
  }
  override def unpackDouble: Double = {
    unpacker.unpackDouble()
  }
  override def unpackString: String = {
    unpacker.unpackString()
  }

  override def unpackTimestamp: Instant = {
    val extTypeHeader = unpacker.unpackExtensionTypeHeader()
    if (extTypeHeader.getType != -1) {
      throw new MessageTypeException(s"Unexpected type: ${extTypeHeader}. Expected -1 (Timestamp)")
    }
    val ext    = unpacker.readPayload(extTypeHeader.getLength)
    val cursor = new ReadCursor(ByteArrayBuffer(ext), 0)
    val instant = OffsetUnpacker.unpackTimestampInternal(
      ExtTypeHeader(extType = extTypeHeader.getType, byteLength = extTypeHeader.getLength),
      cursor)
    instant
  }
  override def unpackArrayHeader: Int = {
    unpacker.unpackArrayHeader()
  }
  override def unpackMapHeader: Int = {
    unpacker.unpackMapHeader()
  }
  override def unpackExtTypeHeader: ExtTypeHeader = {
    val extHeader = unpacker.unpackExtensionTypeHeader()
    ExtTypeHeader(extHeader.getType, extHeader.getLength)
  }
  override def unpackRawStringHeader: Int = {
    unpacker.unpackRawStringHeader()
  }
  override def unpackBinaryHeader: Int = {
    unpacker.unpackBinaryHeader()
  }
  override def unpackValue: Value = {
    UnpackerImpl.fromMsgPackV8Value(unpacker.unpackValue())
  }
//  override def skipPayload(numBytes: Int): Unit                              = {
//    unpacker.skipPayload()
//  }
//
  override def readPayload(dst: Array[Byte]): Unit = {
    unpacker.readPayload(dst)
  }
  override def readPayload(dst: Array[Byte], offset: Int, length: Int): Unit = {
    unpacker.readPayload(dst, offset, length)
  }
  override def readPayload(length: Int): Array[Byte] = {
    unpacker.readPayload(length)
  }
}

object UnpackerImpl {

  private[impl] val conversionTable = {
    import org.msgpack.{core => v8}
    val m = Map.newBuilder[v8.MessageFormat, MessageFormat]
    for (b <- 0 until 0xFF) {
      val f1 = v8.MessageFormat.valueOf(b.toByte)
      val f2 = MessageFormat.of(b.toByte)
      m += f1 -> f2
    }
    m.result
  }

  import org.msgpack.{value => v8}
  import wvlet.airframe.msgpack.spi.Value._
  import scala.collection.JavaConverters._

  def fromMsgPackV8Value(v: v8.Value): Value = {
    v match {
      case v: v8.NilValue                        => NilValue
      case v: v8.BooleanValue                    => BooleanValue(v.getBoolean)
      case v: v8.IntegerValue if v.isInLongRange => LongValue(v.toLong)
      case v: v8.IntegerValue                    => BigIntegerValue(v.toBigInteger)
      case v: v8.FloatValue                      => DoubleValue(v.toDouble)
      case v: v8.StringValue                     => StringValue(v.toString)
      case v: v8.ExtensionValue                  => ExtensionValue(v.getType, v.getData)
      case v: v8.ArrayValue =>
        ArrayValue(v.asScala.map(fromMsgPackV8Value(_)).toIndexedSeq)
      case v: v8.MapValue =>
        MapValue(
          v.entrySet().asScala.map { e =>
              fromMsgPackV8Value(e.getKey) -> fromMsgPackV8Value(e.getValue)
            }.toMap)
    }
  }
}
