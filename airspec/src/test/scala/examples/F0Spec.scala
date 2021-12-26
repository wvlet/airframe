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
package examples

import wvlet.airframe.newDesign
import wvlet.airspec.AirSpec

import java.util.concurrent.atomic.AtomicInteger

/**
  */
object F0Spec extends AirSpec {

  private val f0 = new AtomicInteger(0)
  private val f1 = new AtomicInteger(0)
  private val f2 = new AtomicInteger(0)

  override protected def afterAll: Unit = {
    f0.get() shouldBe 1
    f1.get() shouldBe 1
    f2.get() shouldBe 1
  }

  test("weird one-arg") {
    // https://github.com/wvlet/airframe/issues/1845
    f0.incrementAndGet()
    Nil
  }

  test("one-arg", design = newDesign.bind[String].toInstance("hello")) { (s: String) =>
    f1.incrementAndGet()
  }

  def fun1: Long => Int = { (s: Long) =>
    0
  }

  test("function return")[Unit] {
    f2.incrementAndGet()
    fun1
  }

  test("returns function")[Unit] {
    val f = { (i: Int) => s"count ${i}" }
    f
  }
}
