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

  type MyInt = Int
  type MyMap = Map[Int, String]

  case class D[V](id: Int, v: V)

  trait Service[-Req, +Rep] extends (Req => Future[Rep])

  case class E(a:A)


}

import wvlet.surface.Examples._
/**
  *
  */
class SurfaceTest extends SurfaceSpec {

  "Surface" should {
    "resolve types" in {
      val a = check(Surface.of[A], "A")
      a.isAlias shouldBe false
      a.isOption shouldBe false
      a.isPrimitive shouldBe false

      val b = check(Surface.of[B], "B")
      b.isAlias shouldBe false
      b.isOption shouldBe false
      b.isPrimitive shouldBe false
    }

    "resolve primitive types" taggedAs("primitive") in {
      checkPrimitive(Surface.of[Boolean], "Boolean")
      checkPrimitive(Surface.of[Byte], "Byte")
      checkPrimitive(Surface.of[Short], "Short")
      checkPrimitive(Surface.of[Int], "Int")
      checkPrimitive(Surface.of[Long], "Long")
      checkPrimitive(Surface.of[Float], "Float")
      checkPrimitive(Surface.of[Double], "Double")
      checkPrimitive(Surface.of[String], "String")
      checkPrimitive(Surface.of[Char], "Char")
      checkPrimitive(Surface.of[java.lang.String], "String")
    }

    "resolve surface from class" in {
      pending
      val a = Surface.of[A]
      //check(Surface.get(classOf[A]).get, a.toString)
    }

    "be equal" in {
      val a1 = Surface.of[A]
      val a2 = Surface.of[A]
      a1 shouldBe theSameInstanceAs(a2)
      // equality
      a1 shouldBe a2
      a1.hashCode() shouldBe a2.hashCode()

      val b = Surface.of[B]
      val a3 = b.params.head.surface
      a1 shouldBe theSameInstanceAs(a3)

      // Generic surface
      val c1 = Surface.of[Seq[Int]]
      val c2 = Surface.of[Seq[Int]]
      c1.equals(c2) shouldBe true
      c1 shouldBe theSameInstanceAs(c2)
      c1.hashCode() shouldBe c2.hashCode()

      c1 shouldNot be (a1)
      c1.equals(a1) shouldBe false
      c1.equals("hello") shouldBe false
    }

    "resolve alias" in {
      val a1 = check(Surface.of[MyA], "MyA:=A")
      a1.isAlias shouldBe true
      a1.isOption shouldBe false

      val a2 = check(Surface.of[MyInt], "MyInt:=Int")
      a2.isAlias shouldBe true
      a1.isOption shouldBe false

      val a3 = check(Surface.of[MyMap], "MyMap:=Map[Int,String]")
      a3.isAlias shouldBe true
      a1.isOption shouldBe false
    }

    "resolve trait" in {
      check(Surface.of[C], "C")
    }

    "resolve array types" in {
      check(Surface.of[Array[Int]], "Array[Int]")
      check(Surface.of[Array[Byte]], "Array[Byte]")
      check(Surface.of[Array[A]], "Array[A]")
    }

    "resolve option types" in {
      val opt = check(Surface.of[Option[A]], "Option[A]")
      opt.isOption shouldBe true
    }

    "resolve collection types" in {
      check(Surface.of[Seq[A]], "Seq[A]")
      check(Surface.of[List[A]], "List[A]")
      check(Surface.of[Map[String, A]], "Map[String,A]")
      check(Surface.of[Map[String, Long]], "Map[String,Long]")
      check(Surface.of[Map[Long, B]], "Map[Long,B]")
      check(Surface.of[Set[String]], "Set[String]")
      check(Surface.of[IndexedSeq[A]], "IndexedSeq[A]")
    }

    "resolve scala util types" in {
      check(Surface.of[Either[String, Throwable]], "Either[String,Throwable]")
      check(Surface.of[Try[A]], "Try[A]")
    }


    "resolve mutable Collection types" in {
      check(Surface.of[collection.mutable.Seq[String]], "Seq[String]")
      check(Surface.of[collection.mutable.Map[Int, String]], "Map[Int,String]")
      check(Surface.of[collection.mutable.Set[A]], "Set[A]")
    }

    "resolve tuples" in {
      check(Surface.of[Tuple1[Int]], "Tuple1[Int]")
      check(Surface.of[(Int, String)], "Tuple2[Int,String]")
      check(Surface.of[(Int, String, A, Double)], "Tuple4[Int,String,A,Double]")
    }

    "resolve java colletion type" in {
      check(Surface.of[java.util.List[String]],"List[String]")
      check(Surface.of[java.util.Map[Long, String]], "Map[Long,String]")
      check(Surface.of[java.util.Set[A]],"Set[A]")
    }

    "resolve generic type" in {
      val d1 = check(Surface.of[D[String]],"D[String]")
      val d2 = check(Surface.of[D[A]],"D[A]")
      d1 shouldNot be theSameInstanceAs (d2)
    }

    "resolve recursive type" in {
      check(Surface.of[Service[Int, String]], "Service[Int,String]")
    }

    "resolve generic abstract type" taggedAs("abstract") in {
      val d = check(Surface.of[D[_]],"D[_]")
      d.typeArgs.length shouldBe 1
      check(Surface.of[Map[_, _]],"Map[_,_]")
    }

    val a0 = A(true, 0.toByte, 1.toShort, 10, 20L, 0.1f, 0.2, "hello")

    "generate object factory" in {
      val a = check(Surface.of[A],"A")
      a.objectFactory shouldBe defined

      val a1 = a.objectFactory.map(_.newInstance(Seq(true, 0.toByte, 1.toShort, 10, 20L, 0.1f, 0.2, "hello")))
      info(a1)
      a1.get shouldBe a0

      val e = check(Surface.of[E],"E")
      e.objectFactory shouldBe defined
      val e1 : E = e.objectFactory.map(_.newInstance(Seq(a0))).get.asInstanceOf[E]
      info(e1)
      e1.a shouldBe a0
    }

    "generate concrete object factory" in {
      val d = check(Surface.of[D[String]],"D[String]")
      val d0 = d.objectFactory.map { f =>
        f.newInstance(Seq(1, "leo"))
      }.get
      info(d0)
      d0 shouldBe D(1, "leo")
    }

    "access parameters" taggedAs("accessor") in {
      pending
      val a = Surface.of[A]
      a.params(0).get(a0) shouldBe true
      a.params(3).get(a0) shouldBe 10
      a.params(4).get(a0) shouldBe 20L
      a.params(7).get(a0) shouldBe "hello"
    }
  }
}
