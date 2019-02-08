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
import java.sql.{Time, Timestamp}

import wvlet.airframe.msgpack.spi.{Packer, Unpacker, ValueType}

/**
  *
  */
object JavaSQLTypeCodec {

  object JavaSqlDateCodec extends MessageCodec[java.sql.Date] {
    override def pack(p: Packer, v: java.sql.Date): Unit = {
      // Store string representation of java.sql.Date
      p.packString(v.toString)
    }
    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      val d =
        u.getNextValueType match {
          case ValueType.STRING =>
            java.sql.Date.valueOf(u.unpackString)
          case ValueType.INTEGER =>
            // Milliseconds since 1970-01-01
            val epochMillis = u.unpackLong
            new java.sql.Date(epochMillis)
          case other =>
            throw new MessageCodecException(INVALID_DATA, this, s"Cannot construct java.sql.Data from ${other} type")
        }
      v.setObject(d)
    }
  }

  object JavaSqlTimeCodec extends MessageCodec[java.sql.Time] {
    override def pack(p: Packer, v: Time): Unit = {
      p.packString(v.toString)
    }
    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      val t = u.getNextValueType match {
        case ValueType.STRING =>
          java.sql.Time.valueOf(u.unpackString)
        case ValueType.INTEGER =>
          val epochMillis = u.unpackLong
          new java.sql.Time(epochMillis)
        case other =>
          throw new MessageCodecException(INVALID_DATA, this, s"Cannot construct java.sql.Time from ${other} type")
      }
      v.setObject(t)
    }
  }

  object JavaSqlTimestampCodec extends MessageCodec[java.sql.Timestamp] {
    override def pack(p: Packer, v: Timestamp): Unit = {
      p.packString(v.toString)
    }
    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      val t = u.getNextValueType match {
        case ValueType.STRING =>
          java.sql.Timestamp.valueOf(u.unpackString)
        case ValueType.INTEGER =>
          val epochMillis = u.unpackLong
          new java.sql.Timestamp(epochMillis)
        case other =>
          throw new MessageCodecException(INVALID_DATA, this, s"Cannot construct java.sql.Time from ${other} type")
      }
      v.setObject(t)
    }
  }

}
