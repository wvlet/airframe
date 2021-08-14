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
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil

/**
  */
object ParquetRecordWriterTest extends AirSpec {
  case class MyRecord(id: Int, name: String)

  test("write generic records with a schema") {
    val schema = Parquet.toParquetSchema(Surface.of[MyRecord])

    IOUtil.withTempFile("target/tmp-record", ".parquet") { file =>
      withResource(Parquet.newRecordWriter(file.getPath, schema)) { writer =>
        writer.write(Map("id" -> 1, "name" -> "leo"))
        writer.write(Array(2, "yui"))
        writer.write("""{"id":3, "name":"aina"}""")
        writer.write("""[4, "ruri"]""")
      }

      withResource(Parquet.newReader[Map[String, Any]](file.getPath)) { reader =>
        reader.read() shouldBe Map("id" -> 1, "name" -> "leo")
        reader.read() shouldBe Map("id" -> 2, "name" -> "yui")
        reader.read() shouldBe Map("id" -> 3, "name" -> "aina")
        reader.read() shouldBe Map("id" -> 4, "name" -> "ruri")
        reader.read() shouldBe null
      }
    }
  }
}
