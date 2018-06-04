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

import wvlet.surface.Surface
import scala.language.existentials

trait AirframeException extends Exception { self =>
  def getCode: String           = this.getClass.getSimpleName
  override def toString: String = getMessage
}

object AirframeException {
  case class MISSING_SESSION(cl: Class[_]) extends AirframeException {
    override def getMessage: String =
      s"[$getCode] Session is not found inside ${cl}. You may need to define ${cl} as a trait or to use constructor injection."
  }
  case class CYCLIC_DEPENDENCY(deps: Set[Surface]) extends AirframeException {
    override def getMessage: String = s"[$getCode] ${deps.mkString(" <- ")}"
  }
  case class MISSING_DEPENDENCY(stack: List[Surface]) extends AirframeException {
    override def getMessage: String = s"[$getCode] Binding for ${stack.head} is not found: ${stack.mkString(" <- ")}"
  }
}
