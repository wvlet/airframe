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
package wvlet.airframe.parquet

import org.apache.parquet.io.api.RecordConsumer
import org.apache.parquet.schema.MessageType
import wvlet.airframe.codec.PrimitiveCodec.{AnyCodec, ValueCodec}
import wvlet.airframe.codec.{MessageCodec, MessageCodecException}
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

/**
  * Adjust any input objects into the shape of the Parquet schema
  *
  * @param schema
  */
class ParquetRecordWriter(schema: MessageType, knownSurfaces: Seq[Surface] = Seq.empty) extends LogSupport {
  private val parquetCodec: ParquetWriteCodec = {
    val surface = ParquetSchema.buildSurfaceFromParquetSchema(schema)
    ParquetWriteCodec.parquetCodecOf(schema, surface, ValueCodec).asRoot
  }

  private val codec = new AnyCodec(knownSurfaces = knownSurfaces)

  def pack(obj: Any, recordConsumer: RecordConsumer): Unit = {
    val msgpack =
      try {
        codec.toMsgPack(obj)
      } catch {
        case e: MessageCodecException =>
          throw new IllegalArgumentException(s"Cannot convert the input into MsgPack: ${obj}", e)
      }
    parquetCodec.writeMsgPack(recordConsumer, msgpack)
  }
}
