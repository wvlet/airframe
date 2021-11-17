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

import wvlet.airframe.control.Control.withResource
import wvlet.airframe.msgpack.spi.{Value, ValueFactory}
import wvlet.airframe.surface.Surface
import wvlet.airframe.ulid.ULID
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil

object NestedRecordWriteTest extends AirSpec {

  case class PartitionIndex(id: ULID, c1_stats: ColStats, c2_stats: ColStats)
  case class ColStats(min: Option[Value], max: Option[Value])

  private val schema = Parquet.toParquetSchema(Surface.of[PartitionIndex])

  test("write nested records") {
    debug(schema)

    IOUtil.withTempFile("target/tmp-nested-record", ".parquet") { file =>
      withResource(Parquet.newRecordWriter(file.getPath, schema)) { writer =>
        writer.write(
          Map(
            "id"       -> ULID.newULID,
            "c1_stats" -> ColStats(Some(ValueFactory.newInteger(1)), Some(ValueFactory.newInteger(10))),
            "c2_stats" -> ColStats(None, None)
          )
        )
      }

      withResource(Parquet.newReader[Map[String, Any]](file.getPath)) { reader =>
        val pi = reader.read()
        info(s"record: ${pi}")
      }
    }
  }
}
