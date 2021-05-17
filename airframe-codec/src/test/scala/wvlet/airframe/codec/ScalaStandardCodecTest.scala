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
package wvlet.airframe.codec
import wvlet.airframe.json.JSON
import wvlet.airframe.json.JSON.{JSONArray, JSONObject, JSONString}
import wvlet.airframe.surface.Surface
import wvlet.airframe.ulid.ULID

/**
  */
class ScalaStandardCodecTest extends CodecSpec {
  test("support Option[A]") {
    val v = Some("hello")
    roundtrip(Surface.of[Option[String]], Some("hello"))
    roundtrip[Option[String]](Surface.of[Option[String]], None)
    roundtrip[Option[Int]](Surface.of[Option[Int]], None)
    roundtrip[Option[Seq[Int]]](Surface.of[Option[Seq[Int]]], Some(Seq(1, 2, 3)))
  }

  test("support tuple") {
    roundtrip[Tuple1[String]](Surface.of[Tuple1[String]], Tuple1("hello"))
    roundtrip(Surface.of[(Int, Int)], (1, 2))
    roundtrip(Surface.of[(Int, Int, Int)], (1, 2, 3))
    roundtrip(Surface.of[(Int, String, Boolean, Float)], (1, "a", true, 2.0f))
    roundtrip(Surface.of[(Int, String, Boolean, Float, String)], (1, "a", true, 2.0f, "hello"))
    roundtrip(Surface.of[(Int, String, Boolean, Float, String, Seq[Int])], (1, "a", true, 2.0f, "hello", Seq(1, 3, 4)))

    roundtrip(Surface.of[(Int, Int, Int, Int, Int, Int, Int)], (1, 2, 3, 4, 5, 6, 7))
    roundtrip(Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int)], (1, 2, 3, 4, 5, 6, 7, 8))
    roundtrip(Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int)], (1, 2, 3, 4, 5, 6, 7, 8, 9))
    roundtrip(Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)], (1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
    roundtrip(Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)], (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
    roundtrip(
      Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
    )
    roundtrip(
      Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)
    )
    roundtrip(
      Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
    )
    roundtrip(
      Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
    )
    roundtrip(
      Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
    )
    roundtrip(
      Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)
    )
    roundtrip(
      Surface.of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18)
    )
    roundtrip(
      Surface
        .of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19)
    )
    roundtrip(
      Surface
        .of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
    )
    roundtrip(
      Surface
        .of[(Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)],
      (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21)
    )
  }

  test("support Either Left") {
    val codec   = MessageCodec.of[Either[Throwable, String]]
    val et      = Left(new IllegalArgumentException("test exception"))
    val msgpack = codec.pack(et)
    val either  = codec.unpack(msgpack)
    debug(either)

    either.isLeft shouldBe true
    either.isRight shouldBe false

    either match {
      case Right(_) =>
        fail("Cannot reach here")
      case Left(ex) =>
        ex.getClass shouldBe classOf[GenericException]
        val ge = ex.asInstanceOf[GenericException]
        ge.message shouldBe "test exception"
        ge.exceptionClass shouldBe "java.lang.IllegalArgumentException"
        ge.cause shouldBe None

        // Should generate standard Java stack traces
        val stackTrace = ge.getStackTrace
        if (!isScalaJS) {
          // Stacktrace of Scala.js is not the same with JVM
          val errorLoc = stackTrace.find(x => x.getFileName.contains("ScalaStandardCodecTest"))
          errorLoc match {
            case Some(x) =>
            //x.getMethodName.contains("Left") shouldBe true
            case _ =>
              warn(stackTrace.mkString("\n"))
              fail("should not reach here")
          }
        }
    }
  }

  test("Either Left should produce Array[JSON objects, null]") {
    val codec = MessageCodec.of[Either[Throwable, String]]
    val et    = Left(new IllegalArgumentException("test exception"))
    val json  = codec.toJson(et)
    debug(json)
    JSON.parse(json) match {
      case JSONArray(Seq(obj @ JSONObject(v), JSON.JSONNull)) =>
        (obj / "exceptionClass").toStringValue shouldBe "java.lang.IllegalArgumentException"
        (obj / "message").toStringValue shouldBe "test exception"
      case _ =>
        fail("cannot reach here")
    }
  }

  test("support Either Left with nested exception") {
    val codec   = MessageCodec.of[Either[Throwable, String]]
    val et      = Left(new Exception(new NullPointerException("NPE")))
    val msgpack = codec.pack(et)
    val either  = codec.unpack(msgpack)

    debug(either)
    val json = codec.toJson(et)
    debug(json)

    either match {
      case Left(ex) =>
        ex.getCause match {
          case g @ GenericException("java.lang.NullPointerException", "NPE", stackTrace, None) =>
            // ok
            debug(g)
          case _ =>
            fail("cannot reach here")
        }
      case _ =>
        fail("Cannot reach here")
    }
  }

  test("support Either Right") {
    val codec   = MessageCodec.of[Either[Throwable, String]]
    val msgpack = codec.pack(Right("Hello Either"))
    val either  = codec.unpack(msgpack)
    debug(either)

    either match {
      case Left(_) =>
        fail("should not reach here")
      case Right(s) =>
        s shouldBe "Hello Either"
    }
  }

  test("Either Right should produce JSONArray[null, JSONValue]") {
    val codec = MessageCodec.of[Either[Throwable, String]]
    val json  = codec.toJson(Right("Hello Either"))
    JSON.parse(json) match {
      case JSONArray(Seq(JSON.JSONNull, JSONString(v))) if v == "Hello Either" =>
      // ok
      case _ =>
        fail("cannot reach here")
    }
  }

  test("read valid JSON input for Either") {
    val codec = MessageCodec.of[Either[Throwable, String]]
    codec.unpackJson("""[{"exceptionClass":"java.lang.NullPointerException","message":"NPE"}, null]""")
    codec.unpackJson("""[null, "hello"]""")
  }

  test("reject invalid JSON input for Either") {
    val codec = MessageCodec.of[Either[Throwable, String]]

    def testInvalid(json: String): Unit = {
      intercept[IllegalArgumentException] {
        codec.unpackJson(json)
      }
    }

    testInvalid("""["hello", "hello"]""")
    testInvalid("""[{"exceptionClass":"java.lang.NullPointerException","message":"NPE"}, "hello"]""")
    testInvalid("""[null, null]""")
    testInvalid("""[]""")
    testInvalid("""["hello"]""")
    testInvalid("""[null, "hello", null]""")
    testInvalid("""{"exceptionClass":"java.lang.NullPointerException","message":"NPE"}""")
  }

  test("support ULID") {
    val codec   = MessageCodec.of[ULID]
    val ulid    = ULID.newULID
    val msgpack = codec.toMsgPack(ulid)
    val ulid1   = codec.fromMsgPack(msgpack)
    ulid shouldBe ulid1
  }
}
