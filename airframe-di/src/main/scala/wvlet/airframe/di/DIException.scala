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
package wvlet.airframe.di

import wvlet.airframe.surface.Surface

import scala.language.existentials

trait DIException extends Exception { self =>

  /**
    * Returns the exception type
    */
  def getCode: String           = this.getClass.getSimpleName
  override def toString: String = getMessage
}

object DIException {
  case class CYCLIC_DEPENDENCY(deps: List[Surface], sourceCode: SourceCode) extends DIException {
    override def getMessage: String = s"[$getCode] ${deps.reverse.mkString(" -> ")} at ${sourceCode}"
  }
  case class MISSING_DEPENDENCY(stack: List[Surface], sourceCode: SourceCode) extends DIException {
    override def getMessage: String =
      s"[$getCode] Binding for ${stack.head} at ${sourceCode} is not found: ${stack.mkString(" <- ")}"
  }

  case class SHUTDOWN_FAILURE(cause: Throwable) extends DIException {
    override def getMessage: String = {
      s"[${getCode}] Failure at session shutdown: ${cause.getMessage}"
    }
  }
  case class MULTIPLE_SHUTDOWN_FAILURES(causes: List[Throwable]) extends DIException {
    override def getMessage: String = {
      s"[${getCode}] Multiple failures occurred during session shutdown:\n${causes.map(x => s"  - ${x.getMessage}").mkString("\n")}"
    }
  }
}
