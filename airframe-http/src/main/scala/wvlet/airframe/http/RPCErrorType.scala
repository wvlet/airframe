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

import wvlet.airframe.codec.PackSupport
import wvlet.airframe.msgpack.spi.Packer

/**
  */
sealed trait RPCErrorType extends PackSupport {

  def errorCodePrefix: String

  def errorCodeMin: Int = errorCodeRange._1
  def errorCodeMax: Int = errorCodeRange._2

  def isValidErrorCode(code: Int): Boolean = code <= errorCodeMin && code < errorCodeMax
  def isValidHttpStatus(httpStatus: HttpStatus): Boolean

  /**
    * The error code range [start, end)
    */
  def errorCodeRange: (Int, Int)
  def name: String = toString()

  override def pack(p: Packer): Unit = {
    p.packString(name)
  }
}

object RPCErrorType {
  // User-input and authentication related errors, which are not retryable in general
  case object USER_ERROR extends RPCErrorType {
    override def errorCodePrefix: String    = "U"
    override def errorCodeRange: (Int, Int) = (0x0000, 0x1000)
    override def isValidHttpStatus(httpStatus: HttpStatus): Boolean = {
      httpStatus.isClientError
    }
  }
  // Server internal failures, which are retryable in general
  case object INTERNAL_ERROR extends RPCErrorType {
    override def errorCodePrefix: String    = "I"
    override def errorCodeRange: (Int, Int) = (0x1000, 0x2000)

    override def isValidHttpStatus(httpStatus: HttpStatus): Boolean = {
      httpStatus.isServerError
    }

  }

  // The resource has been exhausted or a per-user quota has reached.
  // The request can be retried after the underlying resource issue is resolved.
  case object RESOURCE_ERROR extends RPCErrorType {
    override def errorCodePrefix: String    = "R"
    override def errorCodeRange: (Int, Int) = (0x2000, 0x3000)

    override def isValidHttpStatus(httpStatus: HttpStatus): Boolean = {
      httpStatus == HttpStatus.TooManyRequests_429
    }
  }

  def all: Seq[RPCErrorType] = Seq(USER_ERROR, INTERNAL_ERROR, RESOURCE_ERROR)

  def unapply(s: String): Option[RPCErrorType] = {
    val name = s.toUpperCase()
    all.find(_.toString == name)
  }
}
