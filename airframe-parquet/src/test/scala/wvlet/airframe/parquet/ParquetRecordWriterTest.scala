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

import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.{MessageType, PrimitiveType, Types}
import wvlet.airframe.codec.PrimitiveCodec.AnyCodec
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil

/**
  */
object ParquetRecordWriterTest extends AirSpec {
  case class MyRecord(id: Int, name: String)
  private val schema = Parquet.toParquetSchema(Surface.of[MyRecord])
  info(schema)

  test("write generic records with a schema") {
    IOUtil.withTempFile("target/tmp-record", ".parquet") { file =>
      withResource(Parquet.newRecordWriter(file.getPath, schema)) { writer =>
        writer.write(Map("id" -> 1, "name" -> "leo"))
        writer.write(Array(2, "yui"))
        writer.write("""{"id":3, "name":"aina"}""")
        writer.write("""[4, "ruri"]""")
        writer.write(AnyCodec.toMsgPack(Map("id" -> 5, "name" -> "xxx")))
      }

      withResource(Parquet.newReader[Map[String, Any]](file.getPath)) { reader =>
        reader.read() shouldBe Map("id" -> 1, "name" -> "leo")
        reader.read() shouldBe Map("id" -> 2, "name" -> "yui")
        reader.read() shouldBe Map("id" -> 3, "name" -> "aina")
        reader.read() shouldBe Map("id" -> 4, "name" -> "ruri")
        reader.read() shouldBe Map("id" -> 5, "name" -> "xxx")
        reader.read() shouldBe null
      }
    }
  }

  test("throw an exception for an invalid input") {
    IOUtil.withTempFile("target/tmp-record-invalid", ".parquet") { file =>
      withResource(Parquet.newRecordWriter(file.getPath, schema)) { writer =>
        intercept[IllegalArgumentException] {
          writer.write("{broken json data}")
        }
        intercept[IllegalArgumentException] {
          writer.write("not a json data")
        }
        intercept[IllegalArgumentException] {
          // Broken MessagePack data
          writer.write(Array[Byte](0x1))
        }
        intercept[IllegalArgumentException] {
          // Insufficient array size
          writer.write(Array(1))
        }
        intercept[IllegalArgumentException] {
          // Too large array size
          writer.write(Array(1, 2, 3))
        }
        intercept[IllegalArgumentException] {
          writer.write(null)
        }
      }
    }
  }

  case class RecordOpt(id: Int, flag: Option[Int] = None)
  private val schema2 = new MessageType(
    "my record",
    Types.required(PrimitiveTypeName.INT32).named("id"),
    Types.optional(PrimitiveTypeName.INT32).named("flag")
  )

  test("write records with Option") {
    IOUtil.withTempFile("target/tmp-record-opt", ".parquet") { file =>
      withResource(Parquet.newRecordWriter(file.getPath, schema2)) { writer =>
        writer.write(RecordOpt(1, Some(1)))
        writer.write(RecordOpt(2, None))
        writer.write("""{"id":"3"}""")
      }

      withResource(Parquet.newReader[Map[String, Any]](file.getPath)) { reader =>
        reader.read() shouldBe Map("id" -> 1, "flag" -> 1)
        reader.read() shouldBe Map("id" -> 2)
        reader.read() shouldBe Map("id" -> 3)
        reader.read() shouldBe null
      }
    }

  }
}
