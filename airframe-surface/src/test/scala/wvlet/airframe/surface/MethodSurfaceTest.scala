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

object MethodExamples {

  class A {
    def hello: String                    = "hello"
    def arg2(i: Int, b: Boolean): String = "arg2"
    def abst[X](v: X): X                 = v
    protected def helloProtected         = "hello"
    private def helloPrivate             = "hello"
    final def helloFinal: String         = "hello"
  }
  type MyA = A

  trait P {
    def helloParent: String = "parent"
  }
  class B extends P

  import scala.reflect.ClassTag
  trait G {
    def generic[E: ClassTag](arg: String): E = {
      new Throwable().asInstanceOf[E]
    }
  }
  class C extends G

  class D {
    def hello(v: String = "hello"): String = v
  }

  trait E {
    def hello(v: String = "default"): String
  }

  trait F {
    def mapInput(m: Map[String, Any]): Unit
  }
}

import wvlet.airframe.surface.MethodExamples.*

/**
  */
class MethodSurfaceTest extends SurfaceSpec {

  test("list methods") {
    val m = Surface.methodsOf[A]
    debug(m.mkString("\n"))

    val hello = m.find(_.name == "hello").get
    assert(hello.isAbstract == false)
    assert(hello.isFinal == false)
    assert(hello.isPrivate == false)
    assert(hello.isPublic == true)
    assert(hello.isProtected == false)
    assert(hello.isStatic == false)

    val arg2 = m.find(_.name == "arg2").get
    assert(arg2.isAbstract == false)
    assert(arg2.isFinal == false)
    assert(arg2.isPrivate == false)
    assert(arg2.isPublic == true)
    assert(arg2.isProtected == false)
    assert(arg2.isStatic == false)

    // Hide protected/private methods
    assert(m.find(_.name == "helloProtected").isEmpty)
    assert(m.find(_.name == "helloPrivate").isEmpty)

    val f = m.find(_.name == "helloFinal").get
    assert(f.isAbstract == false)
    assert(f.isProtected == false)
    assert(f.isPublic == true)
    assert(f.isPrivate == false)
    assert(f.isFinal == true)
    assert(f.isStatic == false)

    val m2 = Surface.methodsOf[MyA]
    debug(m2)
  }

  test("inherit parent methods") {
    val m = Surface.methodsOf[B]
    assert(m.find(_.name == "helloParent").isDefined)
  }

  test("support generic methods") {
    val m = Surface.methodsOf[C]
    assert(m.find(_.name == "generic").isDefined)
  }

  test("find method default parameter") {
    val ms = Surface.methodsOf[D]
    val m  = ms.find(_.name == "hello").get
    assert(m.args.headOption.isDefined)
    val h = m.args.head

    val d = new D
    val v = h.getMethodArgDefaultValue(d)
    if (!isScalaJS) {
      // Scala.js doesn't support reading default method arguments
      v shouldBe Some("hello")
    }

    val msg = m.call(d, "world")
    msg shouldBe "world"
  }

  test("find method default parameter in trait") {
    val ms = Surface.methodsOf[E]
    val m  = ms.find(_.name == "hello").get
    assert(m.args.headOption.isDefined)
    val h = m.args.head
    // FIXME: Fix StaticMethodParameter in CompileTimeSurfaceFactory for Scala 3
    if (!isScalaJS && !isScalaNative && !(isScala3 && isScalaJVM)) {
      h.getDefaultValue shouldBe Some("default")

      val d = new E {
        override def hello(v: String = "yay"): String = v
      }
      val v = h.getMethodArgDefaultValue(d)
      // Scala.js doesn't support reading default method arguments
      v shouldBe Some("yay")
    }
  }

  test("find Any surface from Map[String, Any] method surface") {
    val ms = Surface.methodsOf[F]
    ms.find(_.name == "mapInput") match {
      case Some(m) if m.args.size == 1 =>
        val arg = m.args(0)
        val p1  = arg.surface.typeArgs(1)
        p1.fullName shouldBe "scala.Any"
      case _ =>
        fail("F.mapInput method not found")
    }
  }
}
