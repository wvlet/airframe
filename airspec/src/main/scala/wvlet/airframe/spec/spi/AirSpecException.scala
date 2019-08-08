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

import java.lang.reflect.InvocationTargetException

import sbt.testing.Status
import wvlet.airframe.SourceCode

import scala.annotation.tailrec

/**
  *
  */
trait AirSpecException extends RuntimeException {
  def message: String
  def code: SourceCode
}

case class AssertionFailure(message: String, code: SourceCode) extends AirSpecException
case class Ignored(message: String, code: SourceCode)          extends AirSpecException
case class Pending(message: String, code: SourceCode)          extends AirSpecException
case class Skipped(message: String, code: SourceCode)          extends AirSpecException
case class Cancelled(message: String, code: SourceCode)        extends AirSpecException
object AirSpecException {

  @tailrec private[spec] def findCause(e: Throwable): Throwable = {
    e match {
      case i: InvocationTargetException => findCause(i.getTargetException)
      case _                            => e
    }
  }

  private[spec] def classifyException(e: Throwable): Status = {
    findCause(e) match {
      case a: AssertionFailure => Status.Failure
      case i: Ignored          => Status.Ignored
      case p: Pending          => Status.Pending
      case s: Skipped          => Status.Skipped
      case c: Cancelled        => Status.Canceled
      case other               => Status.Error
    }
  }

}
