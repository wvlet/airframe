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

object SerializationTest extends LogSupport {

  case class A1(v: Int = 0)
  case class App(a1: A1) extends LogSupport {

  }

  def provider1(a1: A1): App = {
    val app = App(a1)
    info(s"Created ${app} from ${a1}")
    app
  }

  val d = Design.blanc
          .bind[A1].toInstance(A1(1))
          .bind[App].toProvider(provider1 _)
}

import wvlet.airframe.SerializationTest._

class SerializationTest extends AirframeSpec {

  "Airframe" should {
    "serialize provider" in {
      val b = d.serialize
      val ds = Design.deserialize(b)
      ds shouldEqual d
    }
  }
}
