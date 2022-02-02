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

package wvlet.airframe.surface

import java.math.BigInteger
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

  case class E(a: A)
  case class F(p0: Int = 10)
}

import wvlet.airframe.surface.Examples._

/**
  */
class SurfaceTest extends SurfaceSpec {
  test("resolve types") {
    val a = check(Surface.of[A], "A")
    assert(a.isAlias == false)
    assert(a.isOption == false)
    assert(a.isPrimitive == false)

    val b = check(Surface.of[B], "B")
    assert(b.isAlias == false)
    assert(b.isOption == false)
    assert(b.isPrimitive == false)
  }

  test("resolve primitive types") {
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

  test("find primitive Surfaces") {
    assertEquals(Primitive(classOf[Int]), Primitive.Int)
  }

  test("resolve surface from class") {
    pendingUntil("Scala.js doesn't support reflection")
    val a = Surface.of[A]
    // check(Surface.get(classOf[A]).get, a.toString)
  }

  test("be equal") {
    val a1 = Surface.of[A]
    val a2 = Surface.of[A]
    assert(a1 eq a2)
    // equality
    assert(a1 == a2)
    assert(a1.hashCode() == a2.hashCode())

    val b  = Surface.of[B]
    val a3 = b.params.head.surface
    assert(a1 eq a3)

    // Generic surface
    val c1 = Surface.of[Seq[Int]]
    val c2 = Surface.of[Seq[Int]]
    assert(c1.equals(c2) == true)
    assert(c1 eq c2)
    assert(c1.hashCode() == c2.hashCode())

    assert(c1 ne a1)
    assert(c1.equals(a1) == false)
    assert(c1.equals("hello") == false)
  }

  test("resolve alias") {
    val a1 = check(Surface.of[MyA], "MyA:=A")
    assert(a1.isAlias == true)
    assert(a1.isOption == false)

    val a2 = check(Surface.of[MyInt], "MyInt:=Int")
    assert(a2.isAlias == true)
    assert(a1.isOption == false)

    val a3 = check(Surface.of[MyMap], "MyMap:=Map[Int,String]")
    assert(a3.isAlias == true)
    assert(a1.isOption == false)
  }

  test("resolve trait") {
    check(Surface.of[C], "C")
  }

  test("resolve array types") {
    check(Surface.of[Array[Int]], "Array[Int]")
    check(Surface.of[Array[Byte]], "Array[Byte]")
    check(Surface.of[Array[A]], "Array[A]")
  }

  test("resolve option types") {
    val opt = check(Surface.of[Option[A]], "Option[A]")
    assert(opt.isOption == true)
  }

  test("resolve collection types") {
    check(Surface.of[Seq[A]], "Seq[A]")
    check(Surface.of[List[A]], "List[A]")
    check(Surface.of[Map[String, A]], "Map[String,A]")
    check(Surface.of[Map[String, Long]], "Map[String,Long]")
    check(Surface.of[Map[Long, B]], "Map[Long,B]")
    check(Surface.of[Set[String]], "Set[String]")
    check(Surface.of[IndexedSeq[A]], "IndexedSeq[A]")
  }

  test("resolve scala util types") {
    check(Surface.of[Either[String, Throwable]], "Either[String,Throwable]")
    check(Surface.of[Try[A]], "Try[A]")
  }

  test("resolve mutable Collection types") {
    check(Surface.of[collection.mutable.Seq[String]], "Seq[String]")
    check(Surface.of[collection.mutable.Map[Int, String]], "Map[Int,String]")
    check(Surface.of[collection.mutable.Set[A]], "Set[A]")
  }

  test("resolve tuples") {
    check(Surface.of[Tuple1[Int]], "Tuple1[Int]")
    check(Surface.of[(Int, String)], "Tuple2[Int,String]")
    check(Surface.of[(Int, String, A, Double)], "Tuple4[Int,String,A,Double]")
  }

  test("resolve java colletion type") {
    check(Surface.of[java.util.List[String]], "List[String]")
    check(Surface.of[java.util.Map[Long, String]], "Map[Long,String]")
    check(Surface.of[java.util.Set[A]], "Set[A]")
  }

  test("resolve generic type") {
    val d1 = check(Surface.of[D[String]], "D[String]")
    val d2 = check(Surface.of[D[A]], "D[A]")
    assert(d1 ne d2, "should not be the same instance")
  }

  test("resolve recursive type") {
    check(Surface.of[Service[Int, String]], "Service[Int,String]")
  }

  test("resolve generic abstract type") {
    val d = check(Surface.of[D[_]], "D[_]")
    assertEquals(d.typeArgs.length, 1)
    check(Surface.of[Map[_, _]], "Map[_,_]")
  }

  val a0 = A(true, 0.toByte, 1.toShort, 10, 20L, 0.1f, 0.2, "hello")

  test("generate object factory") {
    val a = check(Surface.of[A], "A")
    assert(a.objectFactory.isDefined)

    val a1 = a.objectFactory.map(_.newInstance(Seq(true, 0.toByte, 1.toShort, 10, 20L, 0.1f, 0.2, "hello")))
    debug(a1)
    assert(a1.get == a0)

    val e = check(Surface.of[E], "E")
    assert(e.objectFactory.isDefined)
    val e1: E = e.objectFactory.map(_.newInstance(Seq(a0))).get.asInstanceOf[E]
    debug(e1)
    assert(e1.a == a0)
  }

  test("generate concrete object factory") {
    val d  = check(Surface.of[D[String]], "D[String]")
    val d0 = d.objectFactory.map { f => f.newInstance(Seq(1, "leo")) }.get
    debug(d0)
    assert(d0 == D(1, "leo"))
  }

  test("find default parameter") {
    val f = check(Surface.of[F], "F")
    val p = f.params(0)
    assert(p.getDefaultValue.isDefined)
    assert(p.getDefaultValue.get == 10)
  }

  test("access parameters") {
    val a = Surface.of[A]
    assert(a.params(0).get(a0) == true)
    assert(a.params(3).get(a0) == 10)
    assert(a.params(4).get(a0) == 20L)
    assert(a.params(7).get(a0) == "hello")
  }

  test("object factory") {
    val s = Surface.of[F]
    assert(s.objectFactory.isDefined)
    val f = s.objectFactory.map(_.newInstance(Seq(100)))
    assertEquals(f, Some(F(100)))
  }

  test("bigint") {
    Surface.of[BigInt]
    Surface.of[BigInteger]
  }
}
