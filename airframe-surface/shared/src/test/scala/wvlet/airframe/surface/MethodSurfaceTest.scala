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
}

import wvlet.airframe.surface.MethodExamples._

/**
  *
  */
class MethodSurfaceTest extends SurfaceSpec {
  scalaJsSupport

  def `list methods`: Unit = {
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
    m.find(_.name == "helloProtected") shouldBe empty
    m.find(_.name == "helloPrivate") shouldBe empty

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

  def `inherit parent methods`: Unit = {
    val m = Surface.methodsOf[B]
    m.find(_.name == "helloParent") shouldBe defined
  }

  def `support generic methods`: Unit = {
    val m = Surface.methodsOf[C]
    m.find(_.name == "generic") shouldBe defined
  }

  def `find method default parameter`: Unit = {
    val ms = Surface.methodsOf[D]
    val m  = ms.find(_.name == "hello").get
    m.args.headOption shouldBe defined
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
}
