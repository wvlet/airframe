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
  */
object DI_03_InTraitInjection extends App {
  import wvlet.airframe._

  case class MyAppConfig(port: Int = 8080)

  // In-Trait Injection
  trait MyApp extends LogSupport {
    private val config = bind[MyAppConfig]

    def run: Unit = {
      info(s"MyApp2 with ${config}")
    }
  }

  val d = newDesign.noLifeCycleLogging

  // Build MyApp with Default Config
  d.build[MyApp] { app =>
    app.run // port = 8080
  }

  // Bind a custom config
  val d2 = d
    .bind[MyAppConfig].toInstance(MyAppConfig(port = 10010))

  // Build MyApp with the custom config
  d2.build[MyApp] { app =>
    app.run // port = 10010
  }

  //
  // [Don't do this] bind[X] cannot be used inside classes
  // class MyAppCls {
  //    private val config = bind[MyAppConfig]
  // }
}
