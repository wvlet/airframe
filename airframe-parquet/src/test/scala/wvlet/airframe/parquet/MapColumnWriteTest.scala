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

import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil

import scala.util.Using

object MapColumnWriteTest extends AirSpec {

  case class MapRecord(id: Int, m: Map[Int, Long])

  test("write map column") {
    val schema = Parquet.toParquetSchema(Surface.of[MapRecord])
    debug(schema)

    IOUtil.withTempFile("target/tmp-map-record", ".parquet") { file =>
      val data = Seq(
        MapRecord(1, Map(1 -> 10L, 2 -> 20L)),
        MapRecord(2, Map(1 -> 10L, 2 -> 20L, 3 -> 30L))
      )
      Using.resource(Parquet.newWriter[MapRecord](file.getPath)) { writer =>
        data.foreach(writer.write)
      }

      Using.resource(Parquet.newReader[MapRecord](file.getPath)) { reader =>
        val r0 = reader.read()
        debug(s"${r0}")
        val r1 = reader.read()
        debug(s"${r1}")
        r0 shouldBe data(0)
        r1 shouldBe data(1)
      }
    }
  }
}
