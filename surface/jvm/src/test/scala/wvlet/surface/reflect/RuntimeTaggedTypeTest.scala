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

import wvlet.surface.tag._

object RuntimeTaggedTypeTest {
  case class Person(id:Int, name:String)

  trait Employee
  trait Customer
  trait Guest

  type Name = String
}

/**
  *
  */
import RuntimeTaggedTypeTest._
class RuntimeTaggedTypeTest extends SurfaceSpec {
  "RuntimeTaggedType" should {
    "pass sanity check" in {
      val e : Person @@ Employee = new Person(1, "leo").taggedWith[Employee]
      val e2 : Person @@ Guest = new Person(2, "yui")
    }

    "be a reference" in {
      val t = check(RuntimeSurface.of[Person @@ Employee], "Person@@Employee")
      val p = t.dealias
      p.name shouldBe "Person"
      t.isPrimitive shouldBe false
      t.isAlias shouldBe false
      t.isOption shouldBe false
      t.objectFactory shouldBe defined
      t.rawType shouldBe classOf[Person]
      t.typeArgs shouldBe empty
      t.params.mkString(",") shouldBe "id:Int,name:String"

      val n = check(RuntimeSurface.of[Name @@ Employee], "Name@@Employee")
      val name = n.dealias
      name.name shouldBe "String"
      n.isPrimitive shouldBe true
      n.isAlias shouldBe true
      n.isOption shouldBe false
      n.objectFactory shouldBe empty
    }

    "tag tagged type" in {
      check(RuntimeSurface.of[Name @@ Person @@ Employee], "Name@@Person@@Employee")
    }

    "be comparable" in {
      val t1 = check(RuntimeSurface.of[Person @@ Employee], "Person@@Employee")
      val t2 = check(RuntimeSurface.of[Person @@ Customer], "Person@@Customer")
      val t3 = check(RuntimeSurface.of[Person @@ Guest], "Person@@Guest")

      val set = Set(t1, t2)
      set should contain (RuntimeSurface.of[Person @@ Employee])
      set should contain (RuntimeSurface.of[Person @@ Customer])
      set should not contain (RuntimeSurface.of[Person @@ Guest])

      set should contain (t1)
      set should contain (t2)
      set should not contain (t3)

      val c = check(RuntimeSurface.of[Seq[String] @@ Employee], "Seq[String]@@Employee")
      val s = Set(c)
      s should contain (RuntimeSurface.of[Seq[String] @@ Employee])
      s should contain (c)
    }
  }

}
