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
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil

/**
  */
class ParquetQueryTest extends AirSpec {

  case class Record(id: Int, name: String)
  private val r1            = Record(1, "leo")
  private val r2            = Record(2, "yui")
  private def sampleRecords = Seq(r1, r2)

  private def withSampleParquetFile[U](body: String => U): U = {
    IOUtil.withTempFile("tmp_record", ".parquet") { file =>
      withResource(Parquet.newWriter[Record](path = file.getPath)) { writer =>
        sampleRecords.foreach { x =>
          writer.write(x)
        }
      }
      body(file.getPath)
    }
  }

  test("select *") {
    withSampleParquetFile { path =>
      val reader = Parquet.query[Map[String, Any]](path, "select * from _")
      reader.read() shouldBe Map("id" -> r1.id, "name" -> r1.name)
      reader.read() shouldBe Map("id" -> r2.id, "name" -> r2.name)
      reader.read() shouldBe null
    }
  }
}
