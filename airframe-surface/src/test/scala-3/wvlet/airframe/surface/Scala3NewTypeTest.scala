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
import wvlet.airspec.AirSpec

object Scala3NewTypeTest extends AirSpec:
  trait Label1

  test("support intersection type") {
    val s = Surface.of[String & Label1]
    s.name shouldBe "String&Label1"
    s.fullName shouldBe "java.lang.String&wvlet.airframe.surface.Scala3NewTypeTest.Label1"
    s shouldMatch { case i: IntersectionSurface =>
      i.left shouldBe Surface.of[String]
      i.right shouldBe Surface.of[Label1]
    }
    s shouldNotBe Surface.of[String]
  }

  test("support union type") {
    val s = Surface.of[String | Label1]
    s.name shouldBe "String|Label1"
    s.fullName shouldBe "java.lang.String|wvlet.airframe.surface.Scala3NewTypeTest.Label1"
    s shouldMatch { case i: UnionSurface =>
      i.left shouldBe Surface.of[String]
      i.right shouldBe Surface.of[Label1]
    }
    s shouldNotBe Surface.of[String]
  }

  opaque type MyEnv = String

  test("opaque types") {
    val s = Surface.of[MyEnv]
    s.name shouldBe "MyEnv"
    s.fullName shouldBe "wvlet.airframe.surface.Scala3NewTypeTest.MyEnv"
    s shouldNotBe Surface.of[String]
    s.dealias shouldBe Surface.of[String]
  }

  test("opaque type in function types") {
    val s = Surface.of[MyEnv => String]
    s.name shouldBe "Function1[MyEnv,String]"
  }

  case class MyString(env: MyEnv)

  test("opaque type in constructor args") {
    val s = Surface.of[MyString]
    s.name shouldBe "MyString"
    s.params(0).surface.name shouldBe "MyEnv"
  }

  class A:
    def hello(env: MyEnv): String = env

  test("opaque type in method args") {
    val m = Surface.methodsOf[A].find(_.name == "hello")
    m shouldBe defined
    m.get.args(0).surface.name shouldBe "MyEnv"
  }

  object O:
    opaque type InnerOpaque = Double

  test("Opaque types from inner object") {
    val s = Surface.of[O.InnerOpaque]
  }
