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

  case class Record(id: Int, name: String, createdAt: ULID = ULID.newULID)
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

  case class RecordProjection(id: Int, name: String)

  test("SQL over Parquet") {
    withSampleParquetFile { path =>
      test("read all columns") {
        val reader = Parquet.query[Map[String, Any]](path, "select * from _")
        reader.read() shouldBe Map("id" -> r1.id, "name" -> r1.name, "createdAt" -> r1.createdAt.toString)
        reader.read() shouldBe Map("id" -> r2.id, "name" -> r2.name, "createdAt" -> r2.createdAt.toString)
        reader.read() shouldBe null
      }

      test("read single column") {
        val reader = Parquet.query[Map[String, Any]](path, "select id from _")
        reader.read() shouldBe Map("id" -> r1.id)
        reader.read() shouldBe Map("id" -> r2.id)
        reader.read() shouldBe null
      }
      test("read multiple columns") {
        val reader = Parquet.query[Map[String, Any]](path, "select id, createdAt from _")
        reader.read() shouldBe Map("id" -> r1.id, "createdAt" -> r1.createdAt.toString)
        reader.read() shouldBe Map("id" -> r2.id, "createdAt" -> r2.createdAt.toString)
        reader.read() shouldBe null
      }

      test("filter rows with =") {
        test("int") {
          val reader = Parquet.query[RecordProjection](path, "select id, name from _ where id = 2")
          reader.read() shouldBe RecordProjection(id = r2.id, name = r2.name)
          reader.read() shouldBe null
        }
        test("string") {
          val reader = Parquet.query[RecordProjection](path, "select id, name from _ where name = 'yui'")
          reader.read() shouldBe RecordProjection(id = r2.id, name = r2.name)
          reader.read() shouldBe null
        }
      }

      test("filter rows with >") {
        test("int") {
          val reader = Parquet.query[RecordProjection](path, "select id, name from _ where id > 1")
          reader.read() shouldBe RecordProjection(id = r2.id, name = r2.name)
          reader.read() shouldBe null
        }
        test("string") {
          val reader = Parquet.query[RecordProjection](path, "select id, name from _ where name > 'leo'")
          reader.read() shouldBe RecordProjection(id = r2.id, name = r2.name)
          reader.read() shouldBe null
        }
      }

      test("filter rows with !=") {
        test("int") {
          val reader = Parquet.query[RecordProjection](path, "select id, name from _ where id != 1")
          reader.read() shouldBe RecordProjection(id = r2.id, name = r2.name)
          reader.read() shouldBe null
        }
        test("string") {
          val reader = Parquet.query[RecordProjection](path, "select id, name from _ where name != 'leo'")
          reader.read() shouldBe RecordProjection(id = r2.id, name = r2.name)
          reader.read() shouldBe null
        }
      }

      test("filter rows with >=") {
        val reader = Parquet.query[RecordProjection](path, "select id, name from _ where id >= 2")
        reader.read() shouldBe RecordProjection(id = r2.id, name = r2.name)
        reader.read() shouldBe null
      }

      test("filter rows with <=") {
        val reader = Parquet.query[RecordProjection](path, "select id, name from _ where id <= 1")
        reader.read() shouldBe RecordProjection(id = r1.id, name = r1.name)
        reader.read() shouldBe null
      }

      test("filter row group") {
        val reader = Parquet.query[RecordProjection](path, "select id, name from _ where id > 3")
        reader.read() shouldBe null
      }
    }
  }
}
