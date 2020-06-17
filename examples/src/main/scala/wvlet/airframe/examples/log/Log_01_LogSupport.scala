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
package wvlet.airframe.examples.log

import wvlet.airframe.examples.log.Log_01_LogSupport.name
import wvlet.log.LogSupport

/**
  */
object Log_01_LogSupport extends App {
  import wvlet.log.LogSupport

  val name = "airframe-log"

  MyApp
}

object MyApp extends LogSupport {
  info(s"Hello ${name}!")
  warn("This is a warning message")
  debug("Debug messages will not be shown by default")
}
