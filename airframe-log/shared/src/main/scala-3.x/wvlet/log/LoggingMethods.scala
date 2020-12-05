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

/**
  */
trait LoggingMethods extends Serializable {

  protected def error(message: Any): Unit                     = ???
  protected def error(message: Any, cause: Throwable): Unit   = ???
  protected def warn(message: Any): Unit                      = ???
  protected def warn(message: Any, cause: Throwable): Unit    = ???
  protected def info(message: Any): Unit                      = ???
  protected def info(message: Any, cause: Throwable): Unit    = ???
  protected def debug(message: Any): Unit                     = ???
  protected def debug(message: Any, cause: Throwable): Unit   = ???
  protected def trace(message: Any): Unit                     = ???
  protected def trace(message: Any, cause: Throwable): Unit   = ???
  protected def logAt(logLevel: LogLevel, message: Any): Unit = ???
}
