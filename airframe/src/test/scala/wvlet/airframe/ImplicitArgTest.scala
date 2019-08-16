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

import wvlet.airspec.AirSpec

object ImplicitArgTest {

  case class ImplA(a: String)(implicit val b: Int)
  case class ImplB(a: String)(implicit val b: Int)

}

/**
  *
  */
class ImplicitArgTest extends AirSpec {
  scalaJsSupport

  import ImplicitArgTest._

  def `support implicit args`: Unit = {

    val d = newDesign
      .bind[String].toInstance("hello")
      .bind[Int].toInstance(10)
      .bind[ImplB].toSingleton
      .bind.toProvider((a: String, b: Int) => ImplA(a)(b))
      .noLifeCycleLogging

    d.build[ImplA] { a =>
      a.a shouldBe "hello"
      a.b shouldBe 10
    }

    d.build[ImplB] { b =>
      b.a shouldBe "hello"
      b.b shouldBe 10
    }
  }

}
