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

import wvlet.airspec.AirSpec
import wvlet.airframe.rx._

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

class RxTest extends AirSpec {
  private val v = new AtomicBoolean(false)

  override def afterAll: Unit = {
    v.get() shouldBe true
  }

  test("return Rx") {
    Rx.intervalMillis(10).map(_ => v.set(true))
  }

  test("return RxVar") {
    val rv = Rx.variable(1)
    rv := 100
    rv
  }
}
