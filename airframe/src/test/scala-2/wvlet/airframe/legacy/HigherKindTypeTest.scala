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
package wvlet.airframe.legacy

import wvlet.airspec.AirSpec

import scala.language.higherKinds
import wvlet.airframe._

object HigherKindTypeTest {
  trait Holder[M[_]] {
    def hello = "hello"
  }

  trait Task[A]
  trait MyFuture[A]
  trait HolderInterpreted extends Holder[Task]

  val interpreted = new HolderInterpreted {
    override def hello: String = "new interpretation"
  }
  val interpreted2 = new Holder[Task] {
    override def hello: String = "another interpretation"
  }
  val interpreted3 = new Holder[MyFuture] {
    override def hello: String = "third interpretation"
  }
}

import HigherKindTypeTest._

class HigherKindTypeTest extends AirSpec {
  scalaJsSupport

  val d =
    newSilentDesign
      .bind[HolderInterpreted].toInstance(interpreted)
      .bind[Holder[Task]].toInstance(interpreted2)
      .bind[Holder[MyFuture]].toInstance(interpreted3)

  test("support higher kind types") {
    d.build[HolderInterpreted] { repo => repo.hello shouldBe "new interpretation" }

    d.build[Holder[Task]] { repo => repo.hello shouldBe "another interpretation" }

    d.build[Holder[MyFuture]] { repo => repo.hello shouldBe "third interpretation" }
  }
}
