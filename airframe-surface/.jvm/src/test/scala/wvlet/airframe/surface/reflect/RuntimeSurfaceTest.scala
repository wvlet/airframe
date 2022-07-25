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
package wvlet.airframe.surface.reflect

import wvlet.airframe.surface.{SurfaceSpec, secret}

import scala.concurrent.Future
import scala.util.Try
import wvlet.airframe.surface.Surface

object RuntimeExamples {
  case class A(b: Boolean, bt: Byte, st: Short, i: Int, l: Long, f: Float, d: Double, str: String)

  case class B(a: A)
  trait C

  type MyA   = A
  type MyInt = Int
  type MyMap = Map[Int, String]
  case class D[V](id: Int, v: V)

  trait Service[-Req, +Rep] extends (Req => Future[Rep])

  case class E(a: A)

  case class F(p0: Int = 10)

  trait TraitOnly {
    @secret
    def methodInTrait: Unit = {}
  }
}

/**
  */
class RuntimeSurfaceTest extends SurfaceSpec {
  import RuntimeExamples._

  test("resolve types") {
    val a = check(RuntimeSurface.of[A], "A")
    assert(a.isAlias == false)
    assert(a.isOption == false)
    assert(a.isPrimitive == false)

    val b = check(RuntimeSurface.of[B], "B")
    assert(b.isAlias == false)
    assert(b.isOption == false)
    assert(b.isPrimitive == false)
  }

  test("resolve trait type") {
    val s = RuntimeSurface.of[TraitOnly]
    assertEquals(s.isAlias, false)
    assert(s.rawType == classOf[TraitOnly])

    val m = Surface.methodsOf[TraitOnly].head
    assertEquals(m.owner.isAlias, false)
    assert(m.owner.rawType == classOf[TraitOnly])

    assert(m.findAnnotationOf[secret].isDefined)
  }

  test("Find surface from Class[_]") {
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

  test("resolve alias") {
    val a1 = check(RuntimeSurface.of[MyA], "MyA:=A")
    assert(a1.isAlias == true)
    assert(a1.isOption == false)

    val a2 = check(RuntimeSurface.of[MyInt], "MyInt:=Int")
    assert(a2.isAlias == true)
    assert(a1.isOption == false)

    val a3 = check(RuntimeSurface.of[MyMap], "MyMap:=Map[Int,String]")
    assert(a3.isAlias == true)
    assert(a1.isOption == false)
  }

  test("resolve trait") {
    check(RuntimeSurface.of[C], "C")
  }

  test("resolve array types") {
    check(RuntimeSurface.of[Array[Int]], "Array[Int]")
    check(RuntimeSurface.of[Array[Byte]], "Array[Byte]")
    check(RuntimeSurface.of[Array[A]], "Array[A]")
  }

  test("resolve option types") {
    val opt = check(RuntimeSurface.of[Option[A]], "Option[A]")
    assert(opt.isOption == true)
  }

  test("resolve collection types") {
    check(RuntimeSurface.of[Seq[A]], "Seq[A]")
    check(RuntimeSurface.of[List[A]], "List[A]")
    check(RuntimeSurface.of[Map[String, A]], "Map[String,A]")
    check(RuntimeSurface.of[Map[String, Long]], "Map[String,Long]")
    check(RuntimeSurface.of[Map[Long, B]], "Map[Long,B]")
    check(RuntimeSurface.of[Set[String]], "Set[String]")
    check(RuntimeSurface.of[IndexedSeq[A]], "IndexedSeq[A]")
  }

  test("resolve scala util types") {
    check(RuntimeSurface.of[Either[String, Throwable]], "Either[String,Throwable]")
    check(RuntimeSurface.of[Try[A]], "Try[A]")
  }

  test("resolve mutable Collection types") {
    check(RuntimeSurface.of[collection.mutable.Seq[String]], "Seq[String]")
    check(RuntimeSurface.of[collection.mutable.Map[Int, String]], "Map[Int,String]")
    check(RuntimeSurface.of[collection.mutable.Set[A]], "Set[A]")
  }

  test("resolve tuples") {
    check(RuntimeSurface.of[Tuple1[Int]], "Tuple1[Int]")
    check(RuntimeSurface.of[(Int, String)], "Tuple2[Int,String]")
    check(RuntimeSurface.of[(Int, String, A, Double)], "Tuple4[Int,String,A,Double]")
  }

  test("resolve java colletion type") {
    check(RuntimeSurface.of[java.util.List[String]], "List[String]")
    check(RuntimeSurface.of[java.util.Map[Long, String]], "Map[Long,String]")
    check(RuntimeSurface.of[java.util.Set[A]], "Set[A]")
  }

  test("resolve generic type") {
    val d1 = check(RuntimeSurface.of[D[String]], "D[String]")
    val d2 = check(RuntimeSurface.of[D[A]], "D[A]")
    assert(d1 ne d2)
  }

  test("resolve recursive type") {
    check(RuntimeSurface.of[Service[Int, String]], "Service[Int,String]")
  }

  test("resolve generic abstract type") {
    val d = check(RuntimeSurface.of[D[_]], "D[_]")
    assert(d.typeArgs.length == 1)
    check(RuntimeSurface.of[Map[_, _]], "Map[_,_]")
  }

  val a0 = A(true, 0.toByte, 1.toShort, 10, 20L, 0.1f, 0.2, "hello")

  test("generate object factory") {
    val a = check(RuntimeSurface.of[A], "A")
    assert(a.objectFactory.isDefined)

    val a1 = a.objectFactory.map(_.newInstance(Seq(true, 0.toByte, 1.toShort, 10, 20L, 0.1f, 0.2, "hello")))
    debug(a1)
    assert(a1.get == a0)

    val e = check(RuntimeSurface.of[E], "E")
    assert(e.objectFactory.isDefined)
    val e1: E = e.objectFactory.map(_.newInstance(Seq(a0))).get.asInstanceOf[E]
    debug(e1)
    assert(e1.a == a0)
  }

  test("generate concrete object factory") {
    val d  = check(RuntimeSurface.of[D[String]], "D[String]")
    val d0 = d.objectFactory.map { f => f.newInstance(Seq(1, "leo")) }.get
    debug(d0)
    assert(d0 == D(1, "leo"))
  }

  test("find default parameter") {
    val f = check(RuntimeSurface.of[F], "F")
    val p = f.params(0)
    assert(p.getDefaultValue.isDefined)
    assert(p.getDefaultValue.get == 10)
  }

  test("access parameters") {
    // ...
    val a = RuntimeSurface.of[A]
    assert(a.params(0).get(a0) == true)
    assert(a.params(3).get(a0) == 10)
    assert(a.params(4).get(a0) == 20L)
    assert(a.params(7).get(a0) == "hello")
  }
}
