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
import wvlet.log.LogSupport

object SerializationTest extends LogSupport {
  case class A1(v: Int = 0)
  case class App(a1: A1) extends LogSupport

  val a1 = A1(1)

  def provider1(a1: A1): App = {
    val app = App(a1)
    debug(s"Created ${app} from ${a1}")
    app
  }
  val d = newDesign
    .bind[A1].toInstance(A1(1))
    .bind[App].toProvider(provider1 _)
}

import DesignSerializationTest._

class SerializationTest extends AirSpec {
  test("serialize provider") {
    import wvlet.airframe.SerializationTest._
    val b  = serialize(d)
    val ds = deserialize(b)
    ds shouldBe d

    val s = ds.newSession
    s.build[A1] shouldBe A1(1)
    s.build[App] shouldBe App(A1(1))
  }

  test("serialize provider that involves toInstance of local var") {
    import ProviderSerializationExample._
    import ProviderVal._

    val d = newDesign
      .bind[D1].toInstance(d1)
      .bind[D2].toInstance(d2)
      .bind[D3].toInstance(d3)
      .bind[D4].toInstance(d4)
      .bind[D5].toInstance(d5)
      .bind[App].toProvider(provider5 _)

    val b  = serialize(d)
    val ds = deserialize(b)
    ds shouldBe d
  }
}
