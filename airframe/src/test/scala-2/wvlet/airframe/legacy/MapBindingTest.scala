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

import wvlet.log.LogSupport
import wvlet.airframe.surface.tag._
import wvlet.airspec.AirSpec

/**
  */
class MapBindingTest extends AirSpec {
  scalaJsSupport
  import MapBindingTest._

  def `support map binding`: Unit = {
    val d = newSilentDesign.bind[Mapper].toSingleton.bind[String @@ InfoHandler].toInstance("info")

    d.withSession { session =>
      val m = session.build[Mapper]
      m.handle("info")
      m.handle("get")
      m.handle("other")
    }
  }
}

object MapBindingTest {
  trait Handler {
    def handle: String
  }

  trait InfoHandler extends Handler {
    val message = bind[String @@ InfoHandler]

    def handle: String = message
  }

  trait GetHandler extends Handler {
    def handle: String = "get"
  }

  trait RescueHandler extends Handler {
    def handle: String = "other"
  }

  trait Mapper extends LogSupport {
    private val dispatcher: String => Handler = {
      case "info" => bind[InfoHandler]
      case "get"  => bind[GetHandler]
      case _      => bind[RescueHandler]
    }

    def handle(name: String) = {
      val message = dispatcher.apply(name).handle
      debug(message)
    }
  }
}
