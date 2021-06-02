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

import wvlet.log.LogSupport

/**
  */
object ProviderVal {
  case class D1(id: Int)
  case class D2(id: Int)
  case class D3(id: Int)
  case class D4(id: Int)
  case class D5(id: Int)
  case class App(d1: D1 = D1(0), d2: D2 = D2(0), d3: D3 = D3(0), d4: D4 = D4(0), d5: D5 = D5(0)) extends LogSupport {
    debug(s"Created ${toString()}")
  }

  val z1 = D1(0)
  val z2 = D2(0)
  val z3 = D3(0)
  val z4 = D4(0)
  val z5 = D5(0)
}

object ProviderSerializationExample extends Serializable {
  import wvlet.airframe.ProviderVal._

  val d1 = D1(1)
  val d2 = D2(2)
  val d3 = D3(3)
  val d4 = D4(4)
  val d5 = D5(5)

  val providerDesign =
    newDesign
      .bind[D1].toInstance(d1)
      .bind[D2].toInstance(d2)
      .bind[D3].toInstance(d3)
      .bind[D4].toInstance(d4)
      .bind[D5].toInstance(d5)

  def provider1(d1: D1): App                                 = App(d1)
  def provider2(d1: D1, d2: D2): App                         = App(d1, d2)
  def provider3(d1: D1, d2: D2, D3: D3): App                 = App(d1, d2, d3)
  def provider4(d1: D1, d2: D2, d3: D3, d4: D4): App         = App(d1, d2, d3, d4)
  def provider5(d1: D1, d2: D2, d3: D3, d4: D4, d5: D5): App = App(d1, d2, d3, d4, d5)
}
