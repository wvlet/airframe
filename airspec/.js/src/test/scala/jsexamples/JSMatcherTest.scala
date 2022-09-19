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
package jsexamples
import wvlet.airspec.AirSpec

import scala.scalajs.js

object JSMatcherTest {
  class Empty1                        extends js.Object
  class Empty2                        extends js.Object
  class HasStringFoo(val foo: String) extends js.Object
  class HasStringBar(val bar: String) extends js.Object
  class HasIntFoo(val foo: Int)       extends js.Object
}

/**
  */
class JSMatcherTest extends AirSpec {
  import JSMatcherTest._

  test("equal if both are empty") {
    new js.Object() shouldNotBeTheSameInstanceAs new js.Object()
    new Empty1 shouldNotBeTheSameInstanceAs new Empty2
    new js.Object() shouldBe new js.Object()
    new Empty1 shouldBe new Empty2
  }

  test("equal if exact same key and value") {
    new HasStringFoo("foo") shouldNotBeTheSameInstanceAs new HasStringFoo("foo")
    new HasStringFoo("foo") shouldBe new HasStringFoo("foo")

    val a = new js.Object() {
      val a = 1
      val b = true
      val c = ""
      val d = null
      val e = js.undefined
    }
    val b = new js.Object() {
      val a = 1
      val b = true
      val c = ""
      val d = null
      val e = js.undefined
    }
    a shouldBe b
    b shouldBe a
  }

  test("equal if exact same key and value with nested object") {
    val a = new js.Object() {
      val a = 1
      val b = true
      val c = ""
      val d = null
      val e = new HasIntFoo(foo = 1)
      val f = js.Array(1, "a")
    }
    val b = new js.Object() {
      // order different
      val f = js.Array(1, "a")
      val c = ""
      val b = true
      val a = 1
      val e = new js.Object() {
        val foo = 1
      }
      val d = null
    }
    a shouldBe b
    b shouldBe a
    new HasStringFoo("foo") shouldBe new HasStringFoo("foo")
  }

  test("not equal if different key") {
    val a = new js.Object() {
      val a = 1
    }
    val b = new js.Object() {
      val b = 2
    }
    a shouldNotBe b
    b shouldNotBe a
    new HasStringFoo(foo = "foo") shouldNotBe new HasStringBar(bar = "foo")
  }

  test("not equal if different value") {
    test("different vals") {
      val a = new js.Object() {
        val a = 1
      }
      val b = new js.Object() {
        val a = 2
      }
      a shouldNotBe b
      b shouldNotBe a
    }

    test("diffent vals with null or undefined") {
      if (isScalaJS && isScala3) {
        pending("A bug in Scala 3 compiler fails to produce null/undefined fields: #2426")
      }
      val c = new js.Object() {
        val a = null
      }
      val d = new js.Object() {
        val a = js.undefined
      }
      c shouldNotBe d
      d shouldNotBe c
    }

    test("different params") {
      new HasStringFoo("foo") shouldNotBe new HasStringFoo("bar")
    }
  }

  test("not equal if having extra key") {
    test("different params") {
      val a = new js.Object() {
        val a     = 1
        val extra = 42
      }
      val b = new js.Object() {
        val a = 1
      }
      a shouldNotBe b
      b shouldNotBe a
    }

    test("different params with null or undefined") {
      if (isScalaJS && isScala3) {
        pending("A bug in Scala 3 compiler fails to produce null/undefined fields: #2426")
      }
      val c = new js.Object() {
        val a     = 1
        val extra = js.undefined
      }
      val d = new js.Object() {
        val a = 1
      }
      c shouldNotBe d
      d shouldNotBe c
    }
  }

  test("equal if both is null") {
    val a: js.Object = null
    val b: js.Object = null
    a shouldBe b
    b shouldBe a
  }

  test("not equal if either is null") {
    val o: js.Object = null
    new js.Object() shouldNotBe o
    o shouldNotBe new js.Object()
  }

  test("not equal with js.Array") {
    new js.Object() {
      val length = 0
    } shouldNotBe js.Array()
  }

  test("equal if both array have deep-equal elements in same order") {
    js.Array() shouldBe js.Array()
    js.Array(1, "a") shouldBe js.Array(1, "a")
    js.Array(new js.Object()) shouldBe js.Array(new js.Object())
  }

  test("not equal if both array have not-deep-equal elements") {
    js.Array(1) shouldNotBe js.Array(2)
    js.Array("a", 1) shouldNotBe js.Array(1, "a")
  }

  test("equal if no properties but JSON string representation equals") {
    new js.Date(1) shouldBe new js.Date(1)
    new js.Date(1) shouldNotBe new js.Date(2)
  }
}
