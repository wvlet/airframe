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
import java.lang.reflect.InvocationTargetException

import wvlet.airframe.json.JSON
import wvlet.airframe.json.JSON.{JSONArray, JSONObject, JSONString}
import wvlet.airframe.surface.Surface

/**
  */
class ScalaStandardCodecTest extends CodecSpec {
  scalaJsSupport

  def `support Option[A]` : Unit = {
    val v = Some("hello")
    roundtrip(Surface.of[Option[String]], Some("hello"))
    roundtrip[Option[String]](Surface.of[Option[String]], None)
    roundtrip[Option[Int]](Surface.of[Option[Int]], None)
    roundtrip[Option[Seq[Int]]](Surface.of[Option[Seq[Int]]], Some(Seq(1, 2, 3)))
  }

  def `support tuple`: Unit = {
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

  def `support Either Left`: Unit = {
    val codec   = MessageCodec.of[Either[Throwable, String]]
    val et      = Left(new IllegalArgumentException("test exception"))
    val msgpack = codec.pack(et)
    val either  = codec.unpack(msgpack)
    debug(either)

    either.isLeft shouldBe true
    either.isRight shouldBe false

    val ex = either.left.get
    ex.getClass shouldBe classOf[GenericException]
    val ge = ex.asInstanceOf[GenericException]
    ge.message shouldBe "test exception"
    ge.exceptionClass shouldBe "java.lang.IllegalArgumentException"
    ge.cause shouldBe None

    // Should generate standard Java stack traces
    val stackTrace = ge.getStackTrace
    val errorLoc   = stackTrace.find(x => x.getFileName.contains("ScalaStandardCodecTest"))
    errorLoc match {
      case Some(x) =>
        x.getMethodName.contains("Left") shouldBe true
      case _ =>
        warn(stackTrace.mkString("\n"))
    }
  }

  def `Either Left should produce Array[JSON objects, null]` : Unit = {
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

  def `support Either Left with nested exception`: Unit = {
    val codec   = MessageCodec.of[Either[Throwable, String]]
    val et      = Left(new Exception(new NullPointerException("NPE")))
    val msgpack = codec.pack(et)
    val either  = codec.unpack(msgpack)

    debug(either)
    val json = codec.toJson(et)
    debug(json)

    val ex = either.left.get
    ex.getCause match {
      case g @ GenericException("java.lang.NullPointerException", "NPE", stackTrace, None) =>
        // ok
        debug(g)
      case _ =>
        fail("cannot reach here")
    }
  }

  def `support Either Right`: Unit = {
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

  def `Either Right should produce JSONArray[null, JSONValue]` : Unit = {
    val codec = MessageCodec.of[Either[Throwable, String]]
    val json  = codec.toJson(Right("Hello Either"))
    JSON.parse(json) match {
      case JSONArray(Seq(JSON.JSONNull, JSONString(v))) if v == "Hello Either" =>
      // ok
      case _ =>
        fail("cannot reatch here")
    }
  }
}
