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
import wvlet.airframe.control.Resource
import wvlet.airframe.{Design, newDesign}
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil

import java.io.File

/**
  */
object ParquetQueryTest extends AirSpec {

  case class Record(
      id: Int,
      name: String,
      b: Boolean,
      l: Long = -1L,
      f: Float = -1.0f,
      d: Double = -1.0,
      opt: Option[String]
  )
  private val r1            = Record(1, "leo", true, 10L, 1.0f, 1000.0, None)
  private val r2            = Record(2, "yui", false, 20L, 2.0f, 2000.0, Some("opt"))
  private def sampleRecords = Seq(r1, r2)

  override protected def design: Design = {
    newDesign.bind[Resource[File]].toInstance {
      val fileResource = Resource.newTempFile("tmp_record", ".parquet")
      withResource(Parquet.newWriter[Record](path = fileResource.get.getPath)) { writer =>
        sampleRecords.foreach { x =>
          writer.write(x)
        }
      }
      fileResource
    }
  }

  case class RecordProjection(id: Int, b: Boolean)

  test("SQL over Parquet") { (file: Resource[File]) =>
    val path = file.get.getPath
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

    test("read multiple columns with case class") {
      val reader = Parquet.query[RecordProjection](path, "select id, b from _")
      reader.read() shouldBe RecordProjection(r1.id, r1.b)
      reader.read() shouldBe RecordProjection(r2.id, r2.b)
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
        val reader = Parquet.query[Record](path, "select * from _ where f = 2.0")
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
      test("double") {
        val reader = Parquet.query[Record](path, "select * from _ where d = 2000.0")
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
        val reader = Parquet.query[Record](path, "select * from _ where f != 2.0")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
      test("double") {
        val reader = Parquet.query[Record](path, "select * from _ where d != 2000.0")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
    }

    test("filter rows with >") {
      test("int") {
        val reader = Parquet.query[Record](path, "select * from _ where id > 1")
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
      test("long") {
        val reader = Parquet.query[Record](path, "select * from _ where l > 10")
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
      test("string") {
        val reader = Parquet.query[Record](path, "select * from _ where name > 'leo'")
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
      test("float") {
        val reader = Parquet.query[Record](path, "select * from _ where f > 1.1")
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
      test("double") {
        val reader = Parquet.query[Record](path, "select * from _ where d > 1000.0")
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
    }

    test("filter rows with >=") {
      test("int") {
        val reader = Parquet.query[Record](path, "select * from _ where id >= 2")
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
      test("long") {
        val reader = Parquet.query[Record](path, "select * from _ where l >= 11")
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
      test("string") {
        val reader = Parquet.query[Record](path, "select * from _ where name >= 'leoo'")
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
      test("float") {
        val reader = Parquet.query[Record](path, "select * from _ where f >= 1.1")
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
      test("double") {
        val reader = Parquet.query[Record](path, "select * from _ where d >= 1001.0")
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
    }

    test("filter rows with <") {
      test("int") {
        val reader = Parquet.query[Record](path, "select * from _ where id < 2")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
      test("long") {
        val reader = Parquet.query[Record](path, "select * from _ where l < 20")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
      test("string") {
        val reader = Parquet.query[Record](path, "select * from _ where name < 'yui'")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
      test("float") {
        val reader = Parquet.query[Record](path, "select * from _ where f < 1.1")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
      test("double") {
        val reader = Parquet.query[Record](path, "select * from _ where d < 1100.0")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
    }

    test("filter rows with <=") {
      test("int") {
        val reader = Parquet.query[Record](path, "select * from _ where id <= 1")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
      test("long") {
        val reader = Parquet.query[Record](path, "select * from _ where l <= 15")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
      test("string") {
        val reader = Parquet.query[Record](path, "select * from _ where name <= 'yu'")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
      test("float") {
        val reader = Parquet.query[Record](path, "select * from _ where f <= 1.1")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
      test("double") {
        val reader = Parquet.query[Record](path, "select * from _ where d <= 1100.0")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
    }

    test("filter rows with BETWEEN") {
      test("int") {
        val reader = Parquet.query[Record](path, "select * from _ where id between 0 and 1")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
      test("long") {
        val reader = Parquet.query[Record](path, "select * from _ where l between 1 and 15")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
      test("string") {
        val reader = Parquet.query[Record](path, "select * from _ where name between 'le' and 'yu'")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
      test("float") {
        val reader = Parquet.query[Record](path, "select * from _ where f between 0.0 and 1.1")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
      test("double") {
        val reader = Parquet.query[Record](path, "select * from _ where d between 1000.0 and 1100.0")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
    }

    test("filter rows with concatenated conditions") {
      test("AND") {
        val reader = Parquet.query[Record](path, "select * from _ where id = 1 and b = true")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
      test("NOT") {
        val reader = Parquet.query[Record](path, "select * from _ where not (id = 1 and b = true)")
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }

      test("OR") {
        val reader = Parquet.query[Record](path, "select * from _ where id = 1 or b = true")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
      test("AND/OR") {
        val reader = Parquet.query[Record](path, "select * from _ where (id = 1 and b = true) or (name = 'leo')")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
    }

    test("is null") {
      test("int") {
        val reader = Parquet.query[Record](path, "select * from _ where id is null")
        reader.read() shouldBe null
      }
      test("long") {
        val reader = Parquet.query[Record](path, "select * from _ where l is null")
        reader.read() shouldBe null
      }
      test("boolean") {
        val reader = Parquet.query[Record](path, "select * from _ where b is null")
        reader.read() shouldBe null
      }
      test("string") {
        val reader = Parquet.query[Record](path, "select * from _ where name is null")
        reader.read() shouldBe null
      }
      test("float") {
        val reader = Parquet.query[Record](path, "select * from _ where f is null")
        reader.read() shouldBe null
      }
      test("double") {
        val reader = Parquet.query[Record](path, "select * from _ where d is null")
        reader.read() shouldBe null
      }
      test("Option[String]") {
        val reader = Parquet.query[Record](path, "select * from _ where opt is null")
        reader.read() shouldBe r1
        reader.read() shouldBe null
      }
    }

    test("is not null") {
      test("int") {
        val reader = Parquet.query[Record](path, "select * from _ where id is not null")
        reader.read() shouldBe r1
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
      test("long") {
        val reader = Parquet.query[Record](path, "select * from _ where l is not null")
        reader.read() shouldBe r1
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
      test("boolean") {
        val reader = Parquet.query[Record](path, "select * from _ where b is not null")
        reader.read() shouldBe r1
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
      test("string") {
        val reader = Parquet.query[Record](path, "select * from _ where name is not null")
        reader.read() shouldBe r1
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
      test("float") {
        val reader = Parquet.query[Record](path, "select * from _ where f is not null")
        reader.read() shouldBe r1
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
      test("double") {
        val reader = Parquet.query[Record](path, "select * from _ where d is not null")
        reader.read() shouldBe r1
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
      test("Option[String]") {
        val reader = Parquet.query[Record](path, "select * from _ where opt is not null")
        reader.read() shouldBe r2
        reader.read() shouldBe null
      }
    }

    test("filter row group") {
      val reader = Parquet.query[Record](path, "select * from _ where id > 3")
      reader.read() shouldBe null
    }

    test("catch unknown columns") {
      intercept[IllegalArgumentException] {
        Parquet.query[Record](path, "select * from _ where idx =  3")
      }
      intercept[IllegalArgumentException] {
        Parquet.query[Record](path, "select * from _ where idx != 3")
      }
      intercept[IllegalArgumentException] {
        Parquet.query[Record](path, "select * from _ where idx > 3")
      }
      intercept[IllegalArgumentException] {
        Parquet.query[Record](path, "select * from _ where idx >= 3")
      }
      intercept[IllegalArgumentException] {
        Parquet.query[Record](path, "select * from _ where idx < 3")
      }
      intercept[IllegalArgumentException] {
        Parquet.query[Record](path, "select * from _ where idx <= 3")
      }
      intercept[IllegalArgumentException] {
        Parquet.query[Record](path, "select * from _ where idx is null")
      }
      intercept[IllegalArgumentException] {
        Parquet.query[Record](path, "select * from _ where idx is not null")
      }
    }

    test("catch unsupported syntax") {
      intercept[IllegalArgumentException] {
        Parquet.query[Record](path, "select * from _ where id >= pow(2, 10)")
      }
    }
  }
}
