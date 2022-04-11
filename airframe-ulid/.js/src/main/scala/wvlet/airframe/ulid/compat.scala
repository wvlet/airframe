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
package wvlet.airframe.ulid

import scala.util.Random

/**
  */
object compat {
  val random: Random = {
    try {
      // When 'crypto' module is available
      new scala.util.Random(new java.security.SecureRandom())
    } catch {
      case _: Throwable =>
        scala.util.Random
    }
  }

  def sleep(millis: Int): Unit = {
    // no-op as Scala.js has no sleep
  }
}
