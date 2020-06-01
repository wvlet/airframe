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
import wvlet.airframe.metrics.{Count, DataSize, ElapsedTime}
import wvlet.airframe.msgpack.spi.{Packer, Unpacker, ValueType}
import wvlet.airframe.surface.Surface

/**
  * Codecs for airframe-metrics
  */
object MetricsCodec {
  val metricsCodec = Map(
    Surface.of[DataSize]    -> DataSizeCodec,
    Surface.of[ElapsedTime] -> ElapsedTimeCodec,
    Surface.of[Count]       -> CountCodec
  )

  object DataSizeCodec extends MessageCodec[DataSize] {
    override def pack(p: Packer, v: DataSize): Unit = {
      p.packString(v.toString())
    }
    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      u.getNextValueType match {
        case ValueType.STRING =>
          v.setObject(DataSize(u.unpackString))
        case ValueType.INTEGER =>
          v.setObject(DataSize(u.unpackLong))
        case ValueType.FLOAT =>
          v.setObject(DataSize(u.unpackDouble.toLong))
        case other =>
          u.skipValue
          v.setError(throw new IllegalArgumentException(s"invalid type ${other} for DataSize"))
      }
    }
  }

  object ElapsedTimeCodec extends MessageCodec[ElapsedTime] {
    override def pack(p: Packer, v: ElapsedTime): Unit = {
      p.packString(v.toString())
    }

    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      u.getNextValueType match {
        case ValueType.STRING =>
          v.setObject(ElapsedTime(u.unpackString))
        case ValueType.INTEGER =>
          v.setObject(ElapsedTime.succinctNanos(u.unpackLong))
        case ValueType.FLOAT =>
          v.setObject(ElapsedTime.succinctNanos(u.unpackFloat.toLong))
        case other =>
          u.skipValue
          v.setError(throw new IllegalArgumentException(s"invalid type ${other} for ElapsedTime"))
      }
    }
  }

  object CountCodec extends MessageCodec[Count] {
    override def pack(p: Packer, v: Count): Unit = {
      p.packString(v.toString())
    }

    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      u.getNextValueType match {
        case ValueType.STRING =>
          v.setObject(Count(u.unpackString))
        case ValueType.INTEGER =>
          v.setObject(Count.succinct(u.unpackLong))
        case ValueType.FLOAT =>
          v.setObject(Count.succinct(u.unpackFloat.toLong))
        case other =>
          u.skipValue
          v.setError(throw new IllegalArgumentException(s"invalid type ${other} for Count"))
      }
    }
  }
}
