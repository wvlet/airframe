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
package wvlet.airframe.sql

import sun.jvm.hotspot.oops.Metadata
import wvlet.airframe.sql.SQLErrorCode.SQLErrorBuilder

/**
  * A common error definition around SQL processing
  *
  * @param errorCode
  * @param message
  * @param cause
  * @param metadata
  */
case class SQLError(
    errorCode: SQLErrorCode,
    message: String,
    cause: Option[Throwable] = None,
    metadata: Map[String, Any] = Map.empty
) extends Exception(
      s"[${errorCode}] ${message}",
      cause.getOrElse(null)
    )

sealed abstract class SQLErrorCode(val code: Int) {
  def toException(message: String): SQLError = SQLErrorBuilder(errorCode = this).toException(message)
  def toException(message: String, cause: Throwable) =
    SQLErrorBuilder(errorCode = this).withCause(cause).toException(message)

  def withCause(e: Throwable): SQLErrorBuilder = SQLErrorBuilder(errorCode = this, cause = Option(e))
  def withMetadata(metadata: Map[String, Any]) = SQLErrorBuilder(errorCode = this, metadata = metadata)
}

object SQLErrorCode {
  case class SQLErrorBuilder(
      errorCode: SQLErrorCode,
      cause: Option[Throwable] = None,
      metadata: Map[String, Any] = Map.empty
  ) {
    def withCause(e: Throwable): SQLErrorBuilder                  = this.copy(cause = Option(e))
    def withMetadata(metadata: Map[String, Any]): SQLErrorBuilder = this.copy(metadata = metadata)
    def toException(message: String): SQLError                    = SQLError(errorCode, message, cause, metadata)
    def toException(message: String, cause: Throwable): SQLError =
      SQLError(errorCode, message, cause = Option(cause), metadata)
  }

  case object UserError       extends SQLErrorCode(0x00000)
  case object SyntaxError     extends SQLErrorCode(0x00001)
  case object UnknownDataType extends SQLErrorCode(0x00002)
  case object InvalidType     extends SQLErrorCode(0x00003)

  case object InternalError extends SQLErrorCode(0x10000)
}
