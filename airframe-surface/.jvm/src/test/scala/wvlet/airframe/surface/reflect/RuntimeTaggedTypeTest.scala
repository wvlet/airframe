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

import wvlet.airframe.surface.{Surface, SurfaceSpec}

import wvlet.airframe.surface.tag._

object RuntimeTaggedTypeTest {
  case class Person(id: Int, name: String)

  trait Employee
  trait Customer
  trait Guest

  type Name = String
}

/**
  */
import RuntimeTaggedTypeTest._
class RuntimeTaggedTypeTest extends SurfaceSpec {
  test("pass sanity check") {
    val e: Person @@ Employee = new Person(1, "leo").taggedWith[Employee]
    val e2: Person @@ Guest   = new Person(2, "yui")
  }

  test("be a reference") {
    val t = check(RuntimeSurface.of[Person @@ Employee], "Person@@Employee")
    val p = t.dealias
    assert(p.name == "Person")
    assert(t.isPrimitive == false)
    assert(t.isAlias == false)
    assert(t.isOption == false)
    assert(t.objectFactory.isDefined)
    assert(t.rawType == classOf[Person])
    assert(t.typeArgs.isEmpty)
    assert(t.params.mkString(",") == "id:Int,name:String")

    val n    = check(RuntimeSurface.of[Name @@ Employee], "Name@@Employee")
    val name = n.dealias
    assert(name.name == "String")
    assert(n.isPrimitive == true)
    assert(n.isAlias == true)
    assert(n.isOption == false)
    assert(n.objectFactory.isEmpty)
  }

  test("tag tagged type") {
    check(RuntimeSurface.of[Name @@ Person @@ Employee], "Name@@Person@@Employee")
  }

  test("be comparable") {
    val t1 = check(RuntimeSurface.of[Person @@ Employee], "Person@@Employee")
    val t2 = check(RuntimeSurface.of[Person @@ Customer], "Person@@Customer")
    val t3 = check(RuntimeSurface.of[Person @@ Guest], "Person@@Guest")

    val set = Set(t1, t2)
    assert(set.contains(RuntimeSurface.of[Person @@ Employee]))
    assert(set.contains(RuntimeSurface.of[Person @@ Customer]))
    assert(!set.contains(RuntimeSurface.of[Person @@ Guest]))

    assert(set.contains(t1))
    assert(set.contains(t2))
    assert(!set.contains(t3))

    val c = check(RuntimeSurface.of[Seq[String] @@ Employee], "Seq[String]@@Employee")
    val s = Set(c)
    assert(s.contains(RuntimeSurface.of[Seq[String] @@ Employee]))
    assert(s.contains(c))
  }
}
