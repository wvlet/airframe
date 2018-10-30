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
package wvlet.airframe.fluentd
import wvlet.airframe.AirframeSpec
import wvlet.airframe._

/**
  *
  */
class FluencyClientTest extends AirframeSpec {

  "should run Fluency as fluentd client" in {
    val d = fluentd.fluencyDesign

    d.build[FluentdClient] { f =>
      //
    }
  }

  "should support console logging" in {
    val d = fluentd.consoleLoggingDesign

    d.build[FluentdClient] { f =>
      f.emit("data", Map("id" -> 1))
    }

  }

}
