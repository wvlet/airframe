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

/**
  * An example of manually creating a session
  */
object DI_04_Session extends App {
  import wvlet.airframe.*

  class MyApp()

  val d = newDesign

  d.withSession { session => // Session will start here
    // Build a new service using DI
    session.build[MyApp]
  }
  // Session will be closed here

  // Suppress startup/shutdown log messages by using debug log levels.
  val d2 = d.noLifeCycleLogging

  d2.build[MyApp] { myapp =>
    //
  }
}
