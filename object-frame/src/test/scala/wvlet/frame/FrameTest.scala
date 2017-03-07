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
package wvlet.frame


object Examples {

  case class A(
    b:Boolean,
    bt:Byte,
    st:Short,
    i:Int,
    l:Long,
    f:Float,
    d:Double,
    str:String
  )

  case class B(a:A)

  type MyA = A

  trait C

  type MyChrono = java.time.temporal.ChronoUnit

  type MyInt = Int

  case class D[V](id:Int, v:V)

}

import java.io.File
import java.time.temporal.ChronoUnit

import Examples._

import scala.collection.parallel.ParSeq
import scala.util.Try
/**
  *
  */
class FrameTest extends FrameSpec {

  def check(body: => Frame) : Frame = {
    val frame = body
    info(s"[${frame.getClass.getSimpleName}] $frame, ${frame.fullName}")
    frame
  }

  "Frame" should {
    "resolve types" in {
      check(Frame.of[A])
      check(Frame.of[B])
    }

    "be equal" in {
      val a1 = Frame.of[A]
      val a2 = Frame.of[A]
      a1 shouldBe theSameInstanceAs(a2)

      val b = Frame.of[B]
      val a3 = b.params.head.frame
      a1 shouldBe theSameInstanceAs(a3)
    }

    "resolve alias" in {
      check(Frame.of[MyA])
      check(Frame.of[MyInt])
    }

    "resolve trait" in {
      check(Frame.of[C])
    }

    "resolve array types" in {
      check(Frame.of[Array[Int]])
      check(Frame.of[Array[Byte]])
      check(Frame.of[Array[A]])
    }

    "resolve option types" in {
      check(Frame.of[Option[A]])
    }

    "resolve Collection types" in {
      check(Frame.of[Seq[A]])
      check(Frame.of[ParSeq[A]])
      check(Frame.of[List[A]])
      check(Frame.of[Map[String, A]])
      check(Frame.of[Map[String, Long]])
      check(Frame.of[Map[Long, B]])
      check(Frame.of[Set[String]])
      check(Frame.of[IndexedSeq[A]])
    }

    "resolve scala util types" in {
      check(Frame.of[Either[String, Throwable]])
      check(Frame.of[Try[A]])
    }

    "resolve java util type" in {
      check(Frame.of[File])
      check(Frame.of[java.util.Date])
      check(Frame.of[java.time.LocalDate])
      check(Frame.of[java.time.LocalDateTime])
      check(Frame.of[java.time.Instant])
    }

    "resolve java enum type" in {
      check(Frame.of[ChronoUnit])
      check(Frame.of[MyChrono])
    }

    "resolve mutable Collection types" in {
      check(Frame.of[collection.mutable.Seq[String]])
      check(Frame.of[collection.mutable.Map[Int, String]])
      check(Frame.of[collection.mutable.Set[A]])
    }

    "resolve tuples" in {
      check(Frame.of[(Int)])
      check(Frame.of[(Int, String)])
      check(Frame.of[(Int, String, A, Double)])
    }

    "resolve java colletion type" in {
      check(Frame.of[java.util.List[String]])
      check(Frame.of[java.util.Map[Long, String]])
      check(Frame.of[java.util.Set[A]])
    }

    "resolve generic type" in {
      val d1 = check(Frame.of[D[String]])
      val d2 = check(Frame.of[D[A]])
      d1 shouldNot be theSameInstanceAs(d2)
    }

  }
}
