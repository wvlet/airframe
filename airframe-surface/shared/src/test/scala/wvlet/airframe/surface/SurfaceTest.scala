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
  *
  */
class SurfaceTest extends SurfaceSpec {
  scalaJsSupport

  def `resolve types`: Unit = {
    val a = check(Surface.of[A], "A")
    assert(a.isAlias == false)
    assert(a.isOption == false)
    assert(a.isPrimitive == false)

    val b = check(Surface.of[B], "B")
    assert(b.isAlias == false)
    assert(b.isOption == false)
    assert(b.isPrimitive == false)
  }

  def `resolve primitive types`: Unit = {
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

  def `resolve surface from class`: Unit = {
    pendingUntil("Scala.js doesn't support reflection")
    val a = Surface.of[A]
    //check(Surface.get(classOf[A]).get, a.toString)
  }

  def `be equal`: Unit = {
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

  def `resolve alias`: Unit = {
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

  def `resolve trait`: Unit = {
    check(Surface.of[C], "C")
  }

  def `resolve array types`: Unit = {
    check(Surface.of[Array[Int]], "Array[Int]")
    check(Surface.of[Array[Byte]], "Array[Byte]")
    check(Surface.of[Array[A]], "Array[A]")
  }

  def `resolve option types`: Unit = {
    val opt = check(Surface.of[Option[A]], "Option[A]")
    assert(opt.isOption == true)
  }

  def `resolve collection types`: Unit = {
    check(Surface.of[Seq[A]], "Seq[A]")
    check(Surface.of[List[A]], "List[A]")
    check(Surface.of[Map[String, A]], "Map[String,A]")
    check(Surface.of[Map[String, Long]], "Map[String,Long]")
    check(Surface.of[Map[Long, B]], "Map[Long,B]")
    check(Surface.of[Set[String]], "Set[String]")
    check(Surface.of[IndexedSeq[A]], "IndexedSeq[A]")
  }

  def `resolve scala util types`: Unit = {
    check(Surface.of[Either[String, Throwable]], "Either[String,Throwable]")
    check(Surface.of[Try[A]], "Try[A]")
  }

  def `resolve mutable Collection types`: Unit = {
    check(Surface.of[collection.mutable.Seq[String]], "Seq[String]")
    check(Surface.of[collection.mutable.Map[Int, String]], "Map[Int,String]")
    check(Surface.of[collection.mutable.Set[A]], "Set[A]")
  }

  def `resolve tuples`: Unit = {
    check(Surface.of[Tuple1[Int]], "Tuple1[Int]")
    check(Surface.of[(Int, String)], "Tuple2[Int,String]")
    check(Surface.of[(Int, String, A, Double)], "Tuple4[Int,String,A,Double]")
  }

  def `resolve java colletion type`: Unit = {
    check(Surface.of[java.util.List[String]], "List[String]")
    check(Surface.of[java.util.Map[Long, String]], "Map[Long,String]")
    check(Surface.of[java.util.Set[A]], "Set[A]")
  }

  def `resolve generic type`: Unit = {
    val d1 = check(Surface.of[D[String]], "D[String]")
    val d2 = check(Surface.of[D[A]], "D[A]")
    d1 shouldNotBeTheSameInstanceAs d2
  }

  def `resolve recursive type`: Unit = {
    check(Surface.of[Service[Int, String]], "Service[Int,String]")
  }

  def `resolve generic abstract type`: Unit = {
    val d = check(Surface.of[D[_]], "D[_]")
    d.typeArgs.length shouldBe 1
    check(Surface.of[Map[_, _]], "Map[_,_]")
  }

  val a0 = A(true, 0.toByte, 1.toShort, 10, 20L, 0.1f, 0.2, "hello")

  def `generate object factory`: Unit = {
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

  def `generate concrete object factory`: Unit = {
    val d  = check(Surface.of[D[String]], "D[String]")
    val d0 = d.objectFactory.map { f => f.newInstance(Seq(1, "leo")) }.get
    debug(d0)
    assert(d0 == D(1, "leo"))
  }

  def `find default parameter`: Unit = {
    val f = check(Surface.of[F], "F")
    val p = f.params(0)
    assert(p.getDefaultValue.isDefined)
    assert(p.getDefaultValue.get == 10)
  }

  def `access parameters`: Unit = {
    val a = Surface.of[A]
    assert(a.params(0).get(a0) == true)
    assert(a.params(3).get(a0) == 10)
    assert(a.params(4).get(a0) == 20L)
    assert(a.params(7).get(a0) == "hello")
  }
}
