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
import wvlet.airspec.AirSpec

import scala.concurrent.Future
import scala.language.higherKinds

/**
  *
  */
object NamedParameterTest extends AirSpec {

  trait MyService[F[_]] {
    def hello: F[String]
  }

  test("read F[_]") {
    val cl = classOf[MyService[Future]]
    val s  = ReflectSurfaceFactory.ofClass(cl)
    s.toString shouldBe "MyService[F]"

    val m = ReflectSurfaceFactory.methodsOfClass(cl)
    m.headOption shouldBe defined

    val m1 = m.head
    m1.returnType.toString shouldBe "F[String]"
  }
}
