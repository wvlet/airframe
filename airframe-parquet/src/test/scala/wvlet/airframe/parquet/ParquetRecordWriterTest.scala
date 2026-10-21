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

import java.io.IOException

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
        writer.write(AnyCodec.default.toMsgPack(Map("id" -> 5, "name" -> "xxx")))
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
    val invalidInputs: Seq[Any] = Seq(
      "{broken json data}",
      "not a json data",
      // Broken MessagePack data
      Array[Byte](0x1),
      // Insufficient array size
      Array(1),
      // Too large array size
      Array(1, 2, 3),
      null
    )
    // Since parquet-java 1.18, a writer is aborted after any write error, so use a new writer for each input
    invalidInputs.foreach { input =>
      IOUtil.withTempFile("target/tmp-record-invalid", ".parquet") { file =>
        withResource(Parquet.newRecordWriter(file.getPath, schema)) { writer =>
          intercept[IllegalArgumentException] {
            writer.write(input)
          }
          intercept[IOException] {
            writer.write(Map("id" -> 1, "name" -> "leo"))
          }
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
      withResource(Parquet.newRecordWriter(file.getPath, schema2, knownSurfaces = Seq(Surface.of[RecordOpt]))) {
        writer =>
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
