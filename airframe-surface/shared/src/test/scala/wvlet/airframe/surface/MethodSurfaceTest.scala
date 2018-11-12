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
}

import MethodExamples._

/**
  *
  */
class MethodSurfaceTest extends SurfaceSpec {
  "MethodSurface" should {

    "list methods" in {
      val m = Surface.methodsOf[A]
      info(m.mkString("\n"))

      val hello = m.find(_.name == "hello").get
      hello.isAbstract shouldBe false
      hello.isFinal shouldBe false
      hello.isPrivate shouldBe false
      hello.isPublic shouldBe true
      hello.isProtected shouldBe false
      hello.isStatic shouldBe false

      val arg2 = m.find(_.name == "arg2").get
      arg2.isAbstract shouldBe false
      arg2.isFinal shouldBe false
      arg2.isPrivate shouldBe false
      arg2.isPublic shouldBe true
      arg2.isProtected shouldBe false
      arg2.isStatic shouldBe false

      val pro = m.find(_.name == "helloProtected").get
      pro.isAbstract shouldBe false
      pro.isProtected shouldBe true
      pro.isPublic shouldBe false
      pro.isPrivate shouldBe false
      pro.isFinal shouldBe false
      pro.isStatic shouldBe false

      val pri = m.find(_.name == "helloPrivate").get
      pri.isAbstract shouldBe false
      pri.isProtected shouldBe false
      pri.isPublic shouldBe false
      pri.isPrivate shouldBe true
      pri.isFinal shouldBe false
      pri.isStatic shouldBe false

      val f = m.find(_.name == "helloFinal").get
      f.isAbstract shouldBe false
      f.isProtected shouldBe false
      f.isPublic shouldBe true
      f.isPrivate shouldBe false
      f.isFinal shouldBe true
      f.isStatic shouldBe false

      val m2 = Surface.methodsOf[MyA]
      info(m2)
    }
  }
}
