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
import wvlet.airframe.tracing.ChromeTracer

/**
  * You can generate a tracing data of your dependency injection.
  *
  * Trace Event Format: https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/preview
  */
object DI_90_Tracing extends App {
  import wvlet.airframe._

  case class MyAppConfig(port: Int = 8080)

  trait MyApp {
    val config = bind[MyAppConfig]
  }

  val d = newSilentDesign
    .withTracer(ChromeTracer.newTracer("target/trace.json"))

  // DI tracing report will be stored in target/trace.json
  // You can open this file with Google Chrome. Open chrome://tracing, and load the json file.
  d.build[MyApp] { app =>
    //
  }
}
