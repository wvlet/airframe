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
import wvlet.airframe.surface.Surface
import wvlet.airframe.{AirframeException, Design}
import wvlet.airspec.AirSpec

object Scala3TypeDITest extends AirSpec:

  opaque type Env = String

  test("opaque type factory binding"):
    val d = Design.newSilentDesign
      .bind[Env].toInstance("hello")
      .bind[Int].toProvider { (env: Env) => 0 }
      .bind[Long].toProvider[Env] { (env: Env) => 0 }
    d.build[Env]: x =>
      x shouldBe "hello"
    intercept[AirframeException] {
      d.build[String]: x =>
        fail("should not reach here")
    }

    test("resolve explicit provider arg types"):
      d.build[Long] { x =>
        x shouldBe 0
      }

    test("resolve implicit provider arg types"):
      pending(s"Scala 3 eagerly resolves provide function arg types")
      d.build[Int] { x =>
        x shouldBe 0
      }

  case class ThreadManager(numThreads: Int = 1)
  trait Service1
  trait Service2

  test("Use intersection type for factory binding"):
    val t1 = ThreadManager(10)
    val t2 = ThreadManager(20)

    val d = Design.newSilentDesign
      .bind[ThreadManager & Service1].toInstance(t1.asInstanceOf[ThreadManager & Service1])
      .bind[ThreadManager & Service2].toInstance(t2.asInstanceOf[ThreadManager & Service2])
      .bind[Int].toProvider[ThreadManager & Service1] { _.numThreads }
      .bind[Long].toProvider[ThreadManager & Service2] { _.numThreads.toLong }

    d.build[ThreadManager & Service1] { x => x shouldBe t1 }
    d.build[ThreadManager & Service2] { x => x shouldBe t2 }
    d.build[ThreadManager & Service1] { x => x shouldBe t1 }
    d.build[Int] { x => x shouldBe t1.numThreads }
    d.build[Long] { x => x shouldBe t2.numThreads }

  test("Use tagged type for factory binding"):
    import wvlet.airframe.surface.tag.*
    val t1 = ThreadManager(10)
    val t2 = ThreadManager(20)

    val s = Surface.of[ThreadManager @@ Service1]
    debug(s)
    val f = Surface.of[(ThreadManager @@ Service1) => Int]
    debug(f)

    val d = Design.newSilentDesign
      .bind[ThreadManager @@ Service1].toInstance(t1)
      .bind[ThreadManager @@ Service2].toInstance(t2)
      .bind[Int].toProvider[ThreadManager @@ Service1] { _.numThreads }
      .bind[Long].toProvider[ThreadManager @@ Service2] { _.numThreads.toLong }

    d.build[ThreadManager @@ Service1] { x => x shouldBe t1 }
    d.build[ThreadManager @@ Service2] { x => x shouldBe t2 }
    d.build[ThreadManager @@ Service1] { x => x shouldBe t1 }
    d.build[Int] { x => x shouldBe t1.numThreads }
    d.build[Long] { x => x shouldBe t2.numThreads }
