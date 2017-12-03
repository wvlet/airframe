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

import java.{lang => jl}

import wvlet.airframe.msgpack.spi._
import wvlet.airframe.codec.PrimitiveCodec._
import wvlet.log.LogSupport

/**
  *
  */
object JDBCCodec {

  object SQLArrayCodec extends MessageCodec[java.sql.Array] with LogSupport {
    override def pack(p: Packer, v: java.sql.Array): Unit = {
      val elemType = v.getBaseType
      debug(s"elemType:${elemType}, ${java.sql.JDBCType.valueOf(elemType)}")
      val jdbcType    = java.sql.JDBCType.valueOf(elemType)
      val arr: AnyRef = v.getArray
      arr match {
        case a: Array[jl.String] =>
          StringArrayCodec.pack(p, a)
        case a: Array[Int] =>
          IntArrayCodec.pack(p, a)
        case a: Array[Short] =>
          ShortArrayCodec.pack(p, a)
        case a: Array[Long] =>
          LongArrayCodec.pack(p, a)
        case a: Array[Char] =>
          CharArrayCodec.pack(p, a)
        case a: Array[Byte] =>
          ByteArrayCodec.pack(p, a)
        case a: Array[Float] =>
          FloatArrayCodec.pack(p, a)
        case a: Array[Double] =>
          DoubleArrayCodec.pack(p, a)
        case a: Array[Boolean] =>
          BooleanArrayCodec.pack(p, a)
        case a: Array[AnyRef] =>
          debug(s"element class: ${a.head.getClass}")
          throw new UnsupportedOperationException(s"Reading array type of ${arr.getClass} is not supported:\n${a.mkString(", ")}")
        case other =>
          throw new UnsupportedOperationException(s"Reading array type of ${arr.getClass} is not supported: ${arr}")
      }
    }

    override def unpack(u: Unpacker, v: MessageHolder) {
      throw new UnsupportedOperationException(s"unpack SQLArray")
    }
  }

}
