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
package wvlet.airspec.spi

import sbt.testing.Status
import wvlet.airframe.SourceCode
import wvlet.airspec.*

/**
  * Define exceptions that will be used for various test failures
  */
trait AirSpecException extends RuntimeException {
  override def getMessage: String = message
  def message: String
}

trait AirSpecFailureBase extends AirSpecException {
  def code: SourceCode
  def statusLabel: String
}

case class AssertionFailure(message: String, code: SourceCode) extends AirSpecFailureBase {
  override def statusLabel: String = "failed"
}
case class Ignored(message: String, code: SourceCode) extends AirSpecFailureBase {
  override def statusLabel: String = "ignored"
}
case class Pending(message: String, code: SourceCode) extends AirSpecFailureBase {
  override def statusLabel: String = "pending"
}
case class Skipped(message: String, code: SourceCode) extends AirSpecFailureBase {
  override def statusLabel: String = "skipped"
}
case class Cancelled(message: String, code: SourceCode) extends AirSpecFailureBase {
  override def statusLabel: String = "cancelled"
}
case class InterceptException(message: String, code: SourceCode) extends AirSpecFailureBase {
  override def statusLabel: String = "interrupted"
}

case class MissingTestDependency(message: String) extends AirSpecException {}

object AirSpecException {
  private[airspec] def classifyException(e: Throwable): Status = {
    compat.findCause(e) match {
      case a: AssertionFailure => Status.Failure
      case i: Ignored          => Status.Ignored
      case p: Pending          => Status.Pending
      case s: Skipped          => Status.Skipped
      case c: Cancelled        => Status.Canceled
      case other               => Status.Error
    }
  }
}
