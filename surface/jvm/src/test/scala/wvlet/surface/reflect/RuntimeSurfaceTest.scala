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
package wvlet.surface.reflect

import wvlet.surface.{Surface, SurfaceSpec}

import scala.concurrent.Future
import scala.util.Try

object RuntimeExamples {

  case class A(b: Boolean,
               bt: Byte,
               st: Short,
               i: Int,
               l: Long,
               f: Float,
               d: Double,
               str: String)

  case class B(a: A)
  trait C

  type MyA = A
  type MyInt = Int
  type MyMap = Map[Int, String]
  case class D[V](id: Int, v: V)

  trait Service[-Req, +Rep] extends (Req => Future[Rep])

  case class E(a:A)

  case class F(p0:Int=10)
}

/**
  *
  */
class RuntimeSurfaceTest extends SurfaceSpec {

  import RuntimeExamples._

  "RuntimeSurface" should {

    "resolve types" in {
      val a = check(RuntimeSurface.of[A], "A")
      a.isAlias shouldBe false
      a.isOption shouldBe false
      a.isPrimitive shouldBe false

      val b = check(RuntimeSurface.of[B], "B")
      b.isAlias shouldBe false
      b.isOption shouldBe false
      b.isPrimitive shouldBe false
    }

    "Find surface from Class[_]" in {
      checkPrimitive(RuntimeSurface.of[Boolean], "Boolean")
      checkPrimitive(RuntimeSurface.of[Byte], "Byte")
      checkPrimitive(RuntimeSurface.of[Short], "Short")
      checkPrimitive(RuntimeSurface.of[Int], "Int")
      checkPrimitive(RuntimeSurface.of[Long], "Long")
      checkPrimitive(RuntimeSurface.of[Float], "Float")
      checkPrimitive(RuntimeSurface.of[Double], "Double")
      checkPrimitive(RuntimeSurface.of[String], "String")
      checkPrimitive(RuntimeSurface.of[Char], "Char")
      checkPrimitive(RuntimeSurface.of[java.lang.String], "String")
    }

    "resolve alias" in {
      val a1 = check(RuntimeSurface.of[MyA], "MyA:=A")
      a1.isAlias shouldBe true
      a1.isOption shouldBe false

      val a2 = check(RuntimeSurface.of[MyInt], "MyInt:=Int")
      a2.isAlias shouldBe true
      a1.isOption shouldBe false

      val a3 = check(RuntimeSurface.of[MyMap], "MyMap:=Map[Int,String]")
      a3.isAlias shouldBe true
      a1.isOption shouldBe false
    }

    "resolve trait" in {
      check(RuntimeSurface.of[C], "C")
    }

    "resolve array types" in {
      check(RuntimeSurface.of[Array[Int]], "Array[Int]")
      check(RuntimeSurface.of[Array[Byte]], "Array[Byte]")
      check(RuntimeSurface.of[Array[A]], "Array[A]")
    }

    "resolve option types" in {
      val opt = check(RuntimeSurface.of[Option[A]], "Option[A]")
      opt.isOption shouldBe true
    }

    "resolve collection types" in {
      check(RuntimeSurface.of[Seq[A]], "Seq[A]")
      check(RuntimeSurface.of[List[A]], "List[A]")
      check(RuntimeSurface.of[Map[String, A]], "Map[String,A]")
      check(RuntimeSurface.of[Map[String, Long]], "Map[String,Long]")
      check(RuntimeSurface.of[Map[Long, B]], "Map[Long,B]")
      check(RuntimeSurface.of[Set[String]], "Set[String]")
      check(RuntimeSurface.of[IndexedSeq[A]], "IndexedSeq[A]")
    }

    "resolve scala util types" taggedAs("throwable")  in {
      check(RuntimeSurface.of[Either[String, Throwable]], "Either[String,Throwable]")
      check(RuntimeSurface.of[Try[A]], "Try[A]")
    }


    "resolve mutable Collection types" in {
      check(RuntimeSurface.of[collection.mutable.Seq[String]], "Seq[String]")
      check(RuntimeSurface.of[collection.mutable.Map[Int, String]], "Map[Int,String]")
      check(RuntimeSurface.of[collection.mutable.Set[A]], "Set[A]")
    }

    "resolve tuples" in {
      check(RuntimeSurface.of[Tuple1[Int]], "Tuple1[Int]")
      check(RuntimeSurface.of[(Int, String)], "Tuple2[Int,String]")
      check(RuntimeSurface.of[(Int, String, A, Double)], "Tuple4[Int,String,A,Double]")
    }

    "resolve java colletion type" in {
      check(RuntimeSurface.of[java.util.List[String]],"List[String]")
      check(RuntimeSurface.of[java.util.Map[Long, String]], "Map[Long,String]")
      check(RuntimeSurface.of[java.util.Set[A]],"Set[A]")
    }

    "resolve generic type" in {
      val d1 = check(RuntimeSurface.of[D[String]],"D[String]")
      val d2 = check(RuntimeSurface.of[D[A]],"D[A]")
      d1 shouldNot be theSameInstanceAs (d2)
    }

    "resolve recursive type" in {
      check(RuntimeSurface.of[Service[Int, String]], "Service[Int,String]")
    }

    "resolve generic abstract type" taggedAs("abstract") in {
      val d = check(RuntimeSurface.of[D[_]],"D[_]")
      d.typeArgs.length shouldBe 1
      check(RuntimeSurface.of[Map[_, _]],"Map[_,_]")
    }

    val a0 = A(true, 0.toByte, 1.toShort, 10, 20L, 0.1f, 0.2, "hello")

    "generate object factory" in {
      val a = check(RuntimeSurface.of[A],"A")
      a.objectFactory shouldBe defined

      val a1 = a.objectFactory.map(_.newInstance(Seq(true, 0.toByte, 1.toShort, 10, 20L, 0.1f, 0.2, "hello")))
      info(a1)
      a1.get shouldBe a0

      val e = check(RuntimeSurface.of[E],"E")
      e.objectFactory shouldBe defined
      val e1 : E = e.objectFactory.map(_.newInstance(Seq(a0))).get.asInstanceOf[E]
      info(e1)
      e1.a shouldBe a0
    }

    "generate concrete object factory" in {
      val d = check(RuntimeSurface.of[D[String]],"D[String]")
      val d0 = d.objectFactory.map { f =>
        f.newInstance(Seq(1, "leo"))
      }.get
      info(d0)
      d0 shouldBe D(1, "leo")
    }

    "find default parameter" taggedAs("dp") in {
      val f = check(RuntimeSurface.of[F], "F")
      val p = f.params(0)
      p.defaultValue shouldBe defined
      p.defaultValue.get shouldBe 10
    }

    "access parameters" in {
      pending
      val a = RuntimeSurface.of[A]
      a.params(0).get(a0) shouldBe true
    }
  }
}
