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
package wvlet.airframe.examples.launcher

import wvlet.airframe.launcher.{Launcher, argument, command, option}

/**
  */
object Launcher_01_Simple extends App {
  // Define global options in the constructor arguments:
  class MyApp(
      @option(prefix = "-e", description = "Environment (e.g., production, staging)")
      env: String,
      @option(prefix = "-h,--help", description = "Display help messages", isHelp = true)
      displayHelp: Boolean
  ) {
    // Define command-local options in the function arguments:
    @command(description = "Launch a monitor")
    def monitor(@argument(description = "monitor type") tpe: String) = {
      // ...
    }
  }

  // Launch a CUI program defined in MyApp
  Launcher.of[MyApp].execute("-h")

  // Launch a sub command
  Launcher.of[MyApp].execute("monitor demo")
}
