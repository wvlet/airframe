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

import org.apache.parquet.schema.Type.Repetition
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil

/**
  */
object SeqTypeTest extends AirSpec {

  case class Contact(id: Int, address: Seq[String])
  private val schema = Parquet.toParquetSchema(Surface.of[Contact])

  test("seq types") {
    debug(schema)
    schema.getType(0).getName shouldBe "id"
    schema.getType(1).getName shouldBe "address"
    schema.getType(1).isRepetition(Repetition.REPEATED) shouldBe true
  }

  test("write seq type values") {
    IOUtil.withTempFile("target/seq", ".parquet") { file =>
      withResource(Parquet.newWriter[Contact](file.getPath)) { writer =>
        writer.write(Contact(1, Seq("A", "B")))
      }

      withResource(Parquet.newReader[Contact](file.getPath)) { reader =>
        reader.read() shouldBe Contact(1, Seq("A", "B"))
        reader.read() shouldBe null
      }
    }
  }
}
