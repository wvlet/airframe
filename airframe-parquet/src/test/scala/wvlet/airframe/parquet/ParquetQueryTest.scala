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
import wvlet.airframe.ulid.ULID
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil

/**
  */
object ParquetQueryTest extends AirSpec {

  case class Record(id: Int, name: String, b: Boolean, l: Long = -1L, f: Float = -1.0f, d: Double = -1.0)
  private val r1            = Record(1, "leo", true, 10L, 1.0f, 1000.0)
  private val r2            = Record(2, "yui", false, 20L, 2.0f, 2000.0)
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

  case class RecordProjection(id: Int, name: String)

  test("SQL over Parquet") {
    withSampleParquetFile { path =>
      test("read all columns") {
        val reader = Parquet.query[Record](path, "select * from _")
        reader.read() shouldBe r1
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }

      test("read single column") {
        val reader = Parquet.query[Map[String, Any]](path, "select id from _")
        reader.read() shouldBe Map("id" -> r1.id)
        reader.read() shouldBe Map("id" -> r2.id)
        reader.read() shouldBe null
      }
      test("read multiple columns") {
        val reader = Parquet.query[Map[String, Any]](path, "select id, b from _")
        reader.read() shouldBe Map("id" -> r1.id, "b" -> r1.b)
        reader.read() shouldBe Map("id" -> r2.id, "b" -> r2.b)
        reader.read() shouldBe null
      }

      test("filter rows with =") {
        test("int") {
          val reader = Parquet.query[Record](path, "select * from _ where id = 2")
          reader.read() shouldBe r2
          reader.read() shouldBe null
        }
        test("long") {
          val reader = Parquet.query[Record](path, "select * from _ where l = 10")
          reader.read() shouldBe r1
          reader.read() shouldBe null
        }
        test("string") {
          val reader = Parquet.query[Record](path, "select * from _ where name = 'yui'")
          reader.read() shouldBe r2
          reader.read() shouldBe null
        }
        test("boolean true") {
          val reader = Parquet.query[Record](path, "select * from _ where b = true")
          reader.read() shouldBe r1
          reader.read() shouldBe null
        }
        test("boolean false") {
          val reader = Parquet.query[Record](path, "select * from _ where b = false")
          reader.read() shouldBe r2
          reader.read() shouldBe null
        }
        test("float") {
          val reader = Parquet.query[Record](path, "select * from _ where f = REAL '2.0'")
          reader.read() shouldBe r2
          reader.read() shouldBe null
        }
        test("double") {
          val reader = Parquet.query[Record](path, "select * from _ where d = DOUBLE '2000.0'")
          reader.read() shouldBe r2
          reader.read() shouldBe null
        }
      }

      test("filter rows with !=") {
        test("int") {
          val reader = Parquet.query[Record](path, "select * from _ where id != 2")
          reader.read() shouldBe r1
          reader.read() shouldBe null
        }
        test("long") {
          val reader = Parquet.query[Record](path, "select * from _ where l != 10")
          reader.read() shouldBe r2
          reader.read() shouldBe null
        }
        test("string") {
          val reader = Parquet.query[Record](path, "select * from _ where name != 'yui'")
          reader.read() shouldBe r1
          reader.read() shouldBe null
        }
        test("boolean true") {
          val reader = Parquet.query[Record](path, "select * from _ where b != true")
          reader.read() shouldBe r2
          reader.read() shouldBe null
        }
        test("boolean false") {
          val reader = Parquet.query[Record](path, "select * from _ where b != false")
          reader.read() shouldBe r1
          reader.read() shouldBe null
        }
        test("float") {
          val reader = Parquet.query[Record](path, "select * from _ where f != REAL '2.0'")
          reader.read() shouldBe r1
          reader.read() shouldBe null
        }
        test("double") {
          val reader = Parquet.query[Record](path, "select * from _ where d != 2000.0")
          reader.read() shouldBe r1
          reader.read() shouldBe null
        }
      }

      test("filter row group") {
        val reader = Parquet.query[Record](path, "select * from _ where id > 3")
        reader.read() shouldBe null
      }
    }
  }
}
