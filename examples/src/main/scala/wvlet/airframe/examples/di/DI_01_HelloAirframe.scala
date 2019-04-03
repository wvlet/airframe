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
package wvlet.airframe.examples.di
import wvlet.log.LogSupport

/**
  *
  */
object DI_01_HelloAirframe extends App {

  import wvlet.airframe._

  trait MyApp extends LogSupport {
    def run = {
      info("Hello Airframe!")
    }
  }

  // Create an empty design
  val d = newDesign

  // Building MyApp using the design
  d.build[MyApp] { app =>
    app.run
  }
}
