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
package wvlet.airframe.di

import wvlet.airframe.*
import wvlet.airspec.AirSpec

object NewDISyntaxTest extends AirSpec {
  trait A
  case class AImpl() extends A
  case class B(s: String)
  case class D1(n: String)
  case class D3(d1: Int, d2: Long, d3: String)
  case class D4(i: Int, l: Long, d: String, b: B)
  case class D5(i: Int, l: Long, d: String, b: B, c: D3)

  test("new bind syntax") {
    val d = newDesign
      .bindInstance[String]("hello")
      .bindSingleton[B]
      .bindImpl[A, AImpl]
      .bindProvider[String, Int] { (s: String) => s.length }
      .bindProvider { (a: A) => D1(a.toString) }
      .bindProvider { (i: Int, s: String) => (i + s.length).toLong }
      .bindProvider { (a: Int, b: Long, c: String) => D3(a, b, c) }
      .bindProvider { (a: Int, b: Long, c: String, d: B) => D4(a, b, c, d) }
      .bindProvider { (a: Int, b: Long, c: String, d: B, cc: D3) => D5(a, b, c, d, cc) }
      .noLifeCycleLogging

    d.withSession { session =>
      session.build[String] shouldBe "hello"
      session.build[B] shouldBe B("hello")
      session.build[A] shouldBe AImpl()
      session.build[Int] shouldBe 5
      session.build[D1] shouldBe D1("AImpl()")

      session.build[Long] shouldBe 10L
      session.build[D3] shouldBe D3(5, 10, "hello")
      session.build[D4] shouldBe D4(5, 10, "hello", B("hello"))
      session.build[D5] shouldBe D5(5, 10, "hello", B("hello"), D3(5, 10, "hello"))
    }
  }

  test("new bind syntax with context") {
    var started = false
    var closed  = false

    val d = newDesign
      .bindInstance[String]("hello")
      .onStart { (s: String) =>
        started = true
      }
      .onShutdown { (s: String) =>
        closed = true
      }
      .noLifeCycleLogging
      .withSession { session =>
        started shouldBe true
        closed shouldBe false
      }

    started shouldBe true
    closed shouldBe true
  }
}
