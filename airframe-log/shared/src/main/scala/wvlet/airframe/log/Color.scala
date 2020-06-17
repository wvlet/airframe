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
package wvlet.airframe.log

import scala.io.AnsiColor

/**
  */
trait AnsiColorPalette extends AnsiColor {
  final val GRAY           = "\u001b[90m"
  final val BRIGHT_RED     = "\u001b[91m"
  final val BRIGHT_GREEN   = "\u001b[92m"
  final val BRIGHT_YELLOW  = "\u001b[93m"
  final val BRIGHT_BLUE    = "\u001b[94m"
  final val BRIGHT_MAGENTA = "\u001b[95m"
  final val BRIGHT_CYAN    = "\u001b[96m"
  final val BRIGHT_WHITE   = "\u001b[97m"
}
