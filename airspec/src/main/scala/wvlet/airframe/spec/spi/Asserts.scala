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
package wvlet.airframe.spec.spi

import wvlet.airframe.SourceCode

/**
  *
  */
trait Asserts {

  def assert(cond: => Boolean)(implicit code: SourceCode) = {
    if (!cond) {
      throw AssertionFailure("assertion failed", code)
    }
  }

  def assert(cond: => Boolean, message: String)(implicit code: SourceCode) = {
    if (!cond) {
      throw AssertionFailure(message, code)
    }
  }

  def fail(reason: String = "failed")(implicit code: SourceCode): Unit = {
    throw AssertionFailure(reason, code)
  }

  def ignore(reason: String = "ignored")(implicit code: SourceCode): Unit = {
    throw Ignored(reason, code)
  }

}
