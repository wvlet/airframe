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
import wvlet.airframe.json.JSON.JSONValue
import wvlet.airframe.json.{JSON, Json}
import wvlet.airframe.ulid.ULID
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil

import java.time.Instant

/**
  */
object ParquetTest extends AirSpec {

  case class MyEntry(id: Int, name: String)
  private val e1 = MyEntry(1, "leo")
  private val e2 = MyEntry(2, "yui")

  test("write Parquet") {

    IOUtil.withTempFile("target/tmp", ".parquet") { file =>
      debug(s"Writing to ${file}")
      withResource(Parquet.newWriter[MyEntry](path = file.getPath)) { writer =>
        writer.write(e1)
        writer.write(e2)
      }

      withResource(Parquet.newReader[MyEntry](path = file.getPath)) { reader =>
        val r1 = reader.read()
        r1 shouldBe e1
        val r2 = reader.read()
        r2 shouldBe e2
        reader.read() shouldBe null
      }

      withResource(Parquet.newReader[Map[String, Any]](path = file.getPath)) { reader =>
        val r1 = reader.read()
        r1 shouldBe Map("id" -> e1.id, "name" -> e1.name)
        val r2 = reader.read()
        r2 shouldBe Map("id" -> e2.id, "name" -> e2.name)
        reader.read() shouldBe null
      }

      withResource(Parquet.newReader[Json](path = file.getPath)) { reader =>
        reader.read() shouldBe """{"id":1,"name":"leo"}"""
        reader.read() shouldBe """{"id":2,"name":"yui"}"""
        reader.read() shouldBe null
      }

    }
  }

  case class MyData(
      p1: Int = 1,
      p2: Short = 2,
      p3: Long = 3L,
      p4: Float = 4.0f,
      p5: Double = 5.0,
      p6: String = "6",
      p7: Boolean = true,
      p8: Boolean = false,
      json: Json = """{"id":1,"param":"json param"}""",
      jsonValue: JSONValue = JSON.parse("""{"id":1,"param":"json param"}"""),
      id: ULID = ULID.newULID,
      createdAt: Instant = Instant.now()
  )
  val d1 = MyData()
  val d2 = MyData()

  test("write various data types") {
    IOUtil.withTempFile("target/tmp", ".parquet") { file =>
      withResource(Parquet.newWriter[MyData](path = file.getPath)) { writer =>
        writer.write(d1)
        writer.write(d2)
      }

      withResource(Parquet.newReader[MyData](path = file.getPath)) { reader =>
        val r1 = reader.read()
        r1 shouldBe d1
        val r2 = reader.read()
        r2 shouldBe d2
        reader.read() shouldBe null
      }
    }

  }
}
