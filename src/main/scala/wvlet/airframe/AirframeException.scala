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

import wvlet.obj.ObjectType


trait AirframeException extends Exception { self =>
  def getCode = this.getClass.getSimpleName
  override def toString = getMessage
}

object AirframeException {
  case class MISSING_SESSION(cl:ObjectType) extends AirframeException {
    override def getMessage: String = s"[$getCode] ${cl}"
  }
  case class CYCLIC_DEPENDENCY(deps: Set[ObjectType]) extends AirframeException {
    override def getMessage = s"[$getCode] ${deps.mkString(", ")}"
  }
  case class MISSING_DEPENDENCY(stack:List[ObjectType]) extends AirframeException {
    override def getMessage = s"[$getCode] ${stack.mkString(" <- ")}"
  }
}

