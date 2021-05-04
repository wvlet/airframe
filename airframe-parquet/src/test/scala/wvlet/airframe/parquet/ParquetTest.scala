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
object ParquetTest extends AirSpec {

  case class MyEntry(id: Int, name: String)

  test("write Parquet") {
    IOUtil.withTempFile("target/tmp", ".parquet") { file =>
      info(s"Writing to ${file}")
      withResource(Parquet.writer[MyEntry](path = file.getPath)) { writer =>
        writer.write(MyEntry(1, "leo"))
        writer.write(MyEntry(2, "yui"))
      }

      withResource(Parquet.reader[MyEntry](path = file.getPath)) { reader =>
        val e1 = reader.read()
        info(e1)
        val e2 = reader.read()
        info(e2)
        reader.read() shouldBe null
      }
    }
  }
}
