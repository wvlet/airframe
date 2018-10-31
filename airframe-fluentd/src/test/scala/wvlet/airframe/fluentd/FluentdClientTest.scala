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
class FluentdClientTest extends AirframeSpec {

  "should use Fluency as a Fluentd client" in {
    val d = fluentd.withFluency
      .bind[FluentdTag].toInstance(FluentdTag(prefix = "mymetric"))
      .noLifeCycleLogging

    d.build[FluentdClient] { f =>
      //
      f.emit("data", Map("id" -> 1, "event" -> "GET"))
    }
  }

  "should support console logging" in {
    val d =
      fluentd.withConsoleLogging
        .bind[FluentdTag].toInstance(FluentdTag(prefix = "metrics"))
        .noLifeCycleLogging

    d.build[FluentdClient] { f =>
      f.emit("data", Map("id" -> 1, "event" -> "GET"))
    }
  }
}
