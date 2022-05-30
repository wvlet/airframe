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

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.filter2.compat.FilterCompat
import org.apache.parquet.filter2.predicate.FilterApi
import org.apache.parquet.hadoop.ParquetFileWriter
import wvlet.airframe.control.{Control, Resource}
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.json.JSON.JSONValue
import wvlet.airframe.json.{JSON, Json}
import wvlet.airframe.newDesign
import wvlet.airframe.ulid.ULID
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil

import java.io.File
import java.time.Instant
import java.util.UUID

/**
  */
object ParquetTest extends AirSpec {

  case class MyEntry(id: Int, name: String)
  private val e1 = MyEntry(1, "leo")
  private val e2 = MyEntry(2, "yui")

  test(
    "write Parquet",
    design = newDesign.bind[Resource[File]].toInstance(Resource.newTempFile("target/tmp", ".parquet"))
  ) { (parquetFile: Resource[File]) =>
    val file = parquetFile.get
    debug(s"Writing to ${file}")
    withResource(
      Parquet.newWriter[MyEntry](path = file.getPath, config = _.withWriteMode(ParquetFileWriter.Mode.OVERWRITE))
    ) { writer =>
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

    test("read statistics") {
      val stats = Parquet.readStatistics(file.getPath)
      debug(stats)
      stats("id").numNulls shouldBe Some(0)
      stats("id").uncompressedSize shouldBe defined
      stats("id").compressedSize shouldBe defined
      stats("id").minValue shouldBe Some(1)
      stats("id").maxValue shouldBe Some(2)

      stats("name").numNulls shouldBe Some(0)
      stats("name").uncompressedSize shouldBe defined
      stats("name").compressedSize shouldBe defined
      stats("name").minValue shouldBe Some("leo")
      stats("name").maxValue shouldBe Some("yui")
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
      seqValue: Seq[String] = Seq("s1", "s2"),
      mapValue: Map[String, Any] = Map("param1" -> "hello", "feature1" -> true),
      ulid: ULID = ULID.newULID,
      uuid: UUID = UUID.randomUUID(),
      optNone: Option[String] = None,
      optSome: Option[String] = Some("hello option"),
      createdAt: Instant = Instant.now(),
      updatedAt: Option[Instant] = Some(Instant.now().plusMillis(1000)),
      finishedAt: Option[Instant] = None,
      nested: MyEntry = e1,
      nestedOpt: Option[MyEntry] = Some(e1),
      nestedList: Seq[MyEntry] = Seq(e1, e2)
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

  test("row group filter") {

    IOUtil.withTempFile("target/tmp", ".parquet") { file =>
      debug(s"Writing to ${file}")
      withResource(Parquet.newWriter[MyEntry](path = file.getPath)) { writer =>
        writer.write(e1)
        writer.write(e2)
      }

      val filter = FilterCompat.get(FilterApi.eq(FilterApi.intColumn("id"), Integer.valueOf(100)))
      withResource(Parquet.newReader[MyEntry](path = file.getPath, config = _.withFilter(filter))) { reader =>
        reader.read() shouldBe null
      }

      val filter2 = FilterCompat.get(FilterApi.eq(FilterApi.intColumn("id"), Integer.valueOf(2)))
      withResource(Parquet.newReader[MyEntry](path = file.getPath, config = _.withFilter(filter2))) { reader =>
        reader.read() shouldBe e2
        reader.read() shouldBe null
      }
    }
  }

  case class SampleRecord(p1: Int, p2: String, p3: Long)
  private val r1 = SampleRecord(0, "a", 1L)
  private val r2 = SampleRecord(1, "b", 2L)
  case class SampleRecordProjection(p1: Int, p3: Long)

  test("column pruning") {
    IOUtil.withTempFile("target/tmp-column", ".parquet") { file =>
      withResource(Parquet.newWriter[SampleRecord](path = file.getPath)) { writer =>
        writer.write(r1)
        writer.write(r2)
      }

      withResource(Parquet.newReader[SampleRecordProjection](path = file.getPath)) { reader =>
        reader.read() shouldBe SampleRecordProjection(r1.p1, r1.p3)
        reader.read() shouldBe SampleRecordProjection(r2.p1, r2.p3)
        reader.read() shouldBe null
      }
    }
  }

  test("write to S3") {
    skip("Requires a real S3 bucket to test this code")

    val conf = new Configuration()
    conf.set("fs.s3a.aws.credentials.provider", "wvlet.airframe.parquet.CustomCredentialProvider")
    val path = "s3a://leo-prizm-dev/test.parquet"
    withResource(
      Parquet.newWriter[SampleRecord](path = path, hadoopConf = conf)
    ) { writer =>
      writer.write(r1)
      writer.write(r2)
    }

    withResource(Parquet.newReader[SampleRecord](path = path, hadoopConf = conf)) { reader =>
      reader.read() shouldBe r1
      reader.read() shouldBe r2
      reader.read() shouldBe null
    }
    Parquet.readSchema(path, conf)
  }
}

class CustomCredentialProvider extends ProfileCredentialsProvider("engineering")
