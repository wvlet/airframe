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
package wvlet.airframe

object ImplicitArgTest {

  case class ImpA(a: String)(implicit val b: Int)

}

/**
  *
  */
class ImplicitArgTest extends AirframeSpec {
  import ImplicitArgTest._

  "support implicit args" in {

    val d = newDesign
      .bind[String].toInstance("hello")
      .bind[Int].toInstance(10)
      .bind.toProvider[String, Int](ImpA.apply(_)(_))
      .noLifeCycleLogging

    d.build[ImpA] { a =>
      a.a shouldBe "hello"
      a.b shouldBe 10
    }
  }

}
