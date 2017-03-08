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
package wvlet.surface

import java.io.File
import java.time.temporal.ChronoUnit



import scala.collection.parallel.ParSeq
import scala.concurrent.Future
import scala.util.Try

object Examples {

  case class A(
    b: Boolean,
    bt: Byte,
    st: Short,
    i: Int,
    l: Long,
    f: Float,
    d: Double,
    str: String
  )

  case class B(a: A)

  type MyA = A

  trait C

  type MyChrono = java.time.temporal.ChronoUnit

  type MyInt = Int

  case class D[V](id: Int, v: V)

  trait Service[-Req, +Rep] extends (Req => Future[Rep])
}

import wvlet.surface.Examples._
/**
  *
  */
class SurfaceTest extends SurfaceSpec {

  def check(body: => Surface): Surface = {
    val surface = body
    info(s"[${surface.getClass.getSimpleName}] $surface, ${surface.fullName}")
    surface
  }

  "Surface" should {
    "resolve types" in {
      check(Surface.of[A])
      check(Surface.of[B])
    }

    "be equal" in {
      val a1 = Surface.of[A]
      val a2 = Surface.of[A]
      a1 shouldBe theSameInstanceAs(a2)

      val b = Surface.of[B]
      val a3 = b.params.head.surface
      a1 shouldBe theSameInstanceAs(a3)
    }

    "resolve alias" in {
      check(Surface.of[MyA])
      check(Surface.of[MyInt])
    }

    "resolve trait" in {
      check(Surface.of[C])
    }

    "resolve array types" in {
      check(Surface.of[Array[Int]])
      check(Surface.of[Array[Byte]])
      check(Surface.of[Array[A]])
    }

    "resolve option types" in {
      check(Surface.of[Option[A]])
    }

    "resolve Collection types" in {
      check(Surface.of[Seq[A]])
      check(Surface.of[ParSeq[A]])
      check(Surface.of[List[A]])
      check(Surface.of[Map[String, A]])
      check(Surface.of[Map[String, Long]])
      check(Surface.of[Map[Long, B]])
      check(Surface.of[Set[String]])
      check(Surface.of[IndexedSeq[A]])
    }

    "resolve scala util types" in {
      check(Surface.of[Either[String, Throwable]])
      check(Surface.of[Try[A]])
    }

    "resolve java util type" in {
      check(Surface.of[File])
      check(Surface.of[java.util.Date])
      check(Surface.of[java.time.LocalDate])
      check(Surface.of[java.time.LocalDateTime])
      check(Surface.of[java.time.Instant])
    }

    "resolve java enum type" in {
      check(Surface.of[ChronoUnit])
      check(Surface.of[MyChrono])
    }

    "resolve mutable Collection types" in {
      check(Surface.of[collection.mutable.Seq[String]])
      check(Surface.of[collection.mutable.Map[Int, String]])
      check(Surface.of[collection.mutable.Set[A]])
    }

    "resolve tuples" in {
      check(Surface.of[Tuple1[Int]])
      check(Surface.of[(Int, String)])
      check(Surface.of[(Int, String, A, Double)])
    }

    "resolve java colletion type" in {
      check(Surface.of[java.util.List[String]])
      check(Surface.of[java.util.Map[Long, String]])
      check(Surface.of[java.util.Set[A]])
    }

    "resolve generic type" in {
      val d1 = check(Surface.of[D[String]])
      val d2 = check(Surface.of[D[A]])
      d1 shouldNot be theSameInstanceAs (d2)
    }

    "resolve recursive type" in {
      check(Surface.of[Service[Int, String]])
    }

    "resolve generic abstract type" in {
      check(Surface.of[D[_]])
      check(Surface.of[Map[_, _]])
    }

  }
}
