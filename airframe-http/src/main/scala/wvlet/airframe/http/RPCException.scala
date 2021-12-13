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
package wvlet.airframe.http

case class RPCException(
    // Application-specific error code
    errorCode: ErrorCode,
    // Error message
    message: String,
    // Custom data
    metadata: Map[String, Any] = Map.empty
) {
  def code: Int = errorCode.code
}

case class ErrorCode(
    // Application-specific error code
    code: Int,
    // Error code name
    name: String,
    errorType: ErrorType
)

sealed trait ErrorType {
  def name: String = toString
}

object ErrorType {
  case object USER_ERROR     extends ErrorType
  case object INTERNAL_ERROR extends ErrorType

  def all: Seq[ErrorType] = Seq(USER_ERROR, INTERNAL_ERROR)

  def unapply(s: String): Option[ErrorType] = {
    val name = s.toUpperCase()
    all.find(_.toString == name)
  }
}
