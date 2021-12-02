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
    val m1 = Map(
      "id"       -> ULID.newULID,
      "c1_stats" -> ColStats(Some(ValueFactory.newInteger(1)), Some(ValueFactory.newInteger(10))),
      "c2_stats" -> ColStats(None, None)
    )
    val m2 = Map(
      "id"       -> ULID.newULID,
      "c1_stats" -> ColStats(Some(ValueFactory.newInteger(100)), Some(ValueFactory.newInteger(1000))),
      "c2_stats" -> ColStats(Some(ValueFactory.newString("apple")), Some(ValueFactory.newString("zebra")))
    )

    debug(s"write target schema: ${schema}")

    IOUtil.withTempFile("target/tmp-nested-record", ".parquet") { file =>
      withResource(Parquet.newRecordWriter(file.getPath, schema)) { writer =>
        writer.write(m1)
        writer.write(m2)
      }

      withResource(Parquet.newReader[Map[String, Any]](file.getPath)) { reader =>
        val r1 = reader.read()
        debug(s"record: ${r1}")
        r1.get("id").toString shouldBe m1.get("id").toString
        r1.get("c1_stats") shouldBe Some(Map("min" -> 1, "max" -> 10))
        r1.get("c2_stats") shouldBe Some(Map.empty)

        val r2 = reader.read()
        debug(s"record: ${r2}")
        r2.get("id").toString shouldBe m2.get("id").toString
        r2.get("c1_stats") shouldBe Some(Map("min" -> 100, "max" -> 1000))
        r2.get("c2_stats") shouldBe Some(Map("min" -> "apple", "max" -> "zebra"))
      }
    }
  }

  case class DataEntry(id: ULID, location: DataLocation)
  case class DataLocation(path: String)

  test("write nested objects") {
    val lst = (0 until 10).map { i =>
      DataEntry(ULID.newULID, DataLocation(s"path-${i}"))
    }.toSeq

    IOUtil.withTempFile("target/tmp-nested-objects", ".parquet") { file =>
      withResource(Parquet.newWriter[DataEntry](file.getPath)) { writer =>
        lst.foreach(writer.write(_))
      }

      withResource(Parquet.newReader[DataEntry](file.getPath)) { reader =>
        val b = Seq.newBuilder[DataEntry]
        Iterator.continually(reader.read()).takeWhile(_ != null).foreach { item =>
          b += item
        }
        val result = b.result()
        result shouldBe lst
      }
    }
  }

}
