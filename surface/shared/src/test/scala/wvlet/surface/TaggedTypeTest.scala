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
import wvlet.surface.tag._
import wvlet.surface

object TaggedTypeTest {
  case class Person(id: Int, name: String)

  trait Employee
  trait Customer
  trait Guest

  type Name = String
}

import TaggedTypeTest._
class TaggedTypeTest extends SurfaceSpec {

  "TaggedType" should {
    "pass sanity check" in {
      val e: Person @@ Employee = new Person(1, "leo").taggedWith[Employee]
      val e2: Person @@ Guest   = new Person(2, "yui")
    }

    "be a reference" in {
      val t = check(surface.of[Person @@ Employee], "Person@@Employee")
      val p = t.dealias
      p.name shouldBe "Person"
      t.isPrimitive shouldBe false
      t.isAlias shouldBe false
      t.isOption shouldBe false
      t.objectFactory shouldBe defined
      t.rawType shouldBe classOf[Person]
      t.typeArgs shouldBe empty
      t.params.mkString(",") shouldBe "id:Int,name:String"

      val n    = check(surface.of[Name @@ Employee], "Name@@Employee")
      val name = n.dealias
      name.name shouldBe "String"
      n.isPrimitive shouldBe true
      n.isAlias shouldBe true
      n.isOption shouldBe false
      n.objectFactory shouldBe empty
    }

    "tag tagged type" in {
      check(surface.of[Name @@ Person @@ Employee], "Name@@Person@@Employee")
    }

    "be comparable" in {
      val t1 = check(surface.of[Person @@ Employee], "Person@@Employee")
      val t2 = check(surface.of[Person @@ Customer], "Person@@Customer")
      val t3 = check(surface.of[Person @@ Guest], "Person@@Guest")

      val set = Set(t1, t2)
      set should contain(surface.of[Person @@ Employee])
      set should contain(surface.of[Person @@ Customer])
      set should not contain (surface.of[Person @@ Guest])

      set should contain(t1)
      set should contain(t2)
      set should not contain (t3)

      val c = check(surface.of[Seq[String] @@ Employee], "Seq[String]@@Employee")
      val s = Set(c)
      s should contain(surface.of[Seq[String] @@ Employee])
      s should contain(c)
    }
  }
}
