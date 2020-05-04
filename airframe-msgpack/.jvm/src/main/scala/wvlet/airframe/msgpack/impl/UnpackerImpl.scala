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

import org.msgpack.core.{MessageInsufficientBufferException, MessageIntegerOverflowException, MessageUnpacker}
import wvlet.airframe.msgpack.io.ByteArrayBuffer
import wvlet.airframe.msgpack.spi.Value.{ExtensionValue, TimestampValue}
import wvlet.airframe.msgpack.spi._

import scala.collection.immutable.ListMap

/**
  * A bridge implementation with msgpack-core MessageUnpacker.
  * TODO: Use pure-Scala impl
  */
class UnpackerImpl(unpacker: MessageUnpacker) extends Unpacker {
  override def close(): Unit = {
    unpacker.close()
  }

  override def hasNext: Boolean = {
    unpacker.hasNext
  }
  override def getNextFormat: MessageFormat = {
    try {
      val mf = unpacker.getNextFormat
      UnpackerImpl.conversionTable(mf)
    } catch {
      case e: MessageInsufficientBufferException =>
        throw InsufficientBufferException(unpacker.getTotalReadBytes, 1)
    }
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

  /**
    * Convert MessagePack exceptions into msgpack SPI's one
    *
    * @param body
    * @tparam A
    * @return
    */
  private def wrapOverflowException[A](body: => A): A = {
    try {
      body
    } catch {
      case e: MessageIntegerOverflowException =>
        throw IntegerOverflowException(e.getBigInteger)
    }
  }

  override def unpackNil: Unit = {
    unpacker.unpackNil()
  }
  override def tryUnpackNil: Boolean = {
    unpacker.tryUnpackNil()
  }
  override def unpackBoolean: Boolean = {
    wrapOverflowException {
      unpacker.unpackBoolean()
    }
  }
  override def unpackByte: Byte = {
    wrapOverflowException {
      unpacker.unpackByte()
    }
  }
  override def unpackShort: Short = {
    wrapOverflowException {
      unpacker.unpackShort()
    }
  }
  override def unpackInt: Int = {
    wrapOverflowException {
      unpacker.unpackInt()
    }
  }
  override def unpackLong: Long = {
    wrapOverflowException {
      unpacker.unpackLong()
    }
  }
  override def unpackBigInteger: BigInteger = {
    wrapOverflowException {
      unpacker.unpackBigInteger()
    }
  }
  override def unpackFloat: Float = {
    wrapOverflowException {
      unpacker.unpackFloat()
    }
  }
  override def unpackDouble: Double = {
    wrapOverflowException {
      unpacker.unpackDouble()
    }
  }
  override def unpackString: String = {
    unpacker.unpackString()
  }

  override def unpackTimestamp: Instant = {
    val extHeader = unpacker.unpackExtensionTypeHeader()
    unpackTimestamp(ExtTypeHeader(extHeader.getType, extHeader.getLength))
  }

  override def unpackTimestamp(extTypeHeader: ExtTypeHeader): Instant = {
    val buf  = new Array[Byte](extTypeHeader.byteLength)
    val data = unpacker.readPayload(buf)
    OffsetUnpacker.unpackTimestamp(extTypeHeader, ReadCursor(ByteArrayBuffer(buf), 0))
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

  override def unpackExtValue(extTypeHeader: ExtTypeHeader): Value = {
    if (extTypeHeader.extType == -1) {
      TimestampValue(unpackTimestamp(extTypeHeader))
    } else {
      val extBytes = unpacker.readPayload(extTypeHeader.byteLength)
      ExtensionValue(extTypeHeader.extType, extBytes)
    }
  }

  override def unpackRawStringHeader: Int = {
    unpacker.unpackRawStringHeader()
  }
  override def unpackBinaryHeader: Int = {
    unpacker.unpackBinaryHeader()
  }
  override def unpackValue: Value = {
    wrapOverflowException {
      UnpackerImpl.fromMsgPackV8Value(unpacker.unpackValue())
    }
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
    for (b <- 0 until 0xff) {
      val f1 = v8.MessageFormat.valueOf(b.toByte)
      val f2 = MessageFormat.of(b.toByte)
      m += f1 -> f2
    }
    m.result
  }

  import org.msgpack.{value => v8}
  import wvlet.airframe.msgpack.spi.Value._

  import scala.jdk.CollectionConverters._

  def fromMsgPackV8Value(v: v8.Value): Value = {
    v match {
      case v: v8.NilValue                        => NilValue
      case v: v8.BooleanValue                    => BooleanValue(v.getBoolean)
      case v: v8.IntegerValue if v.isInLongRange => LongValue(v.toLong)
      case v: v8.IntegerValue                    => BigIntegerValue(v.toBigInteger)
      case v: v8.FloatValue                      => DoubleValue(v.toDouble)
      case v: v8.StringValue                     => StringValue(v.toString)
      case v: v8.BinaryValue                     => BinaryValue(v.asByteArray())
      case v: v8.ExtensionValue                  => ExtensionValue(v.getType, v.getData)
      case v: v8.ArrayValue =>
        ArrayValue(v.asScala.map(fromMsgPackV8Value(_)).toIndexedSeq)
      case v: v8.MapValue =>
        // Use ListMap to maintain key-value pair orders
        val m = ListMap.newBuilder[Value, Value]
        for (it <- v.getKeyValueArray().sliding(2, 2)) {
          val kv = it.toIndexedSeq
          m += fromMsgPackV8Value(kv(0)) -> fromMsgPackV8Value(kv(1))
        }
        MapValue(m.result())
    }
  }
}
