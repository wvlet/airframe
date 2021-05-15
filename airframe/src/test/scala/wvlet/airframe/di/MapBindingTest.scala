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

import wvlet.airframe.Design
import wvlet.log.LogSupport
import wvlet.airspec.AirSpec

/**
  */
class MapBindingTest extends AirSpec {
  import MapBindingTest._

  test("support map binding") {
    val d = Design.newSilentDesign
      .bind[Mapper].toSingleton
      .bind[InfoMessage].toInstance("info")

    d.build[Mapper] { m =>
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

  type InfoMessage = String

  class InfoHandler(message: InfoMessage) extends Handler {
    def handle: String = message
  }

  class GetHandler extends Handler {
    def handle: String = "get"
  }

  class RescueHandler extends Handler {
    def handle: String = "other"
  }

  class Mapper(infoHandler: InfoHandler, getHandler: GetHandler, rescueHandler: RescueHandler) extends LogSupport {
    private val dispatcher: String => Handler = {
      case "info" => infoHandler
      case "get"  => getHandler
      case _      => rescueHandler
    }

    def handle(name: String) = {
      val message = dispatcher.apply(name).handle
      debug(message)
    }
  }
}
