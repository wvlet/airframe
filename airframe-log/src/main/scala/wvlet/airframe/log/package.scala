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
package wvlet.airframe
import wvlet.log.LogFormatter.PlainSourceCodeLogFormatter
import wvlet.log.Logger

/**
  */
package object log {

  /**
    * Initialize the logger to the default log level and
    * default handler
    */
  def init: Unit = {
    Logger.init
  }

  /**
    * Initialize the logger without using ANSI coloring
    */
  def initNoColor: Unit = {
    Logger.init
    Logger.setDefaultFormatter(PlainSourceCodeLogFormatter)
  }
}
