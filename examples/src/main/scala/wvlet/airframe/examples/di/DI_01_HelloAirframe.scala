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
  * A basic example of three-step DI: Bind - Design - Build
  */
object DI_01_HelloAirframe extends App {
  import wvlet.airframe._

  case class MyAppConfig(name: String)

  trait MyApp extends LogSupport {
    // Bind a configuration
    private val config = bind[MyAppConfig]

    def run: Unit = {
      info(s"Hello ${config.name}!")
    }
  }

  // Create an empty design
  val d = newDesign
    .bind[MyAppConfig].toInstance(MyAppConfig(name = "Airframe"))

  // Building MyApp using the design
  d.build[MyApp] { app =>
    app.run // Hello Airframe! will be shown
  }
}
