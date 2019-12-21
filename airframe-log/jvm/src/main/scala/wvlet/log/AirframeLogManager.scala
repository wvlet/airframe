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
package wvlet.log
import java.util.logging.LogManager

object AirframeLogManager {
  private[log] var instance: Option[AirframeLogManager] = None

  private[wvlet] def resetFinally: Unit = {
    instance.map(_.reset0())
    instance = None
  }
}

/***
  * Custom log manager to postpone the reset of loggers
  * This is based on the technique mentioned in:
  * https://stackoverflow.com/questions/13825403/java-how-to-get-logger-to-work-in-shutdown-hook
  */
class AirframeLogManager extends LogManager {
  AirframeLogManager.instance = Some(this)

  override def reset(): Unit = {
    // Don't reset yet
  }

  private[log] def reset0(): Unit = { super.reset }
}
