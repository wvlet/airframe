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
  * RPC status types
  */
sealed trait RPCStatusType extends PackSupport {

  def codeStringPrefix: String

  def minCode: Int = codeRange._1
  def maxCode: Int = codeRange._2

  def isValidCode(code: Int): Boolean = minCode <= code && code < maxCode
  def isValidHttpStatus(httpStatus: HttpStatus): Boolean

  /**
    * The error code range [start, end)
    */
  def codeRange: (Int, Int)
  def name: String = toString()

  override def pack(p: Packer): Unit = {
    p.packString(name)
  }
}

object RPCStatusType {

  def ofPrefix(prefix: Char): RPCStatusType = {
    val p = prefix.toString
    all
      .find(_.codeStringPrefix == p).getOrElse(
        throw new IllegalArgumentException(s"Unknown RPCStatusType code prefix: ${prefix}")
      )
  }

  // For successful responses
  case object SUCCESS extends RPCStatusType {
    override def codeStringPrefix: String = "S"
    override def codeRange: (Int, Int)    = (0x0000, 0x1000)
    override def isValidHttpStatus(httpStatus: HttpStatus): Boolean = {
      httpStatus.isSuccessful
    }
  }

  // User-input or authentication related errors, which are not retryable in general
  case object USER_ERROR extends RPCStatusType {
    override def codeStringPrefix: String = "U"
    override def codeRange: (Int, Int)    = (0x1000, 0x2000)
    override def isValidHttpStatus(httpStatus: HttpStatus): Boolean = {
      httpStatus.isClientError
    }
  }
  // Server internal failures, which are retryable in general
  case object INTERNAL_ERROR extends RPCStatusType {
    override def codeStringPrefix: String = "I"
    override def codeRange: (Int, Int)    = (0x2000, 0x3000)

    override def isValidHttpStatus(httpStatus: HttpStatus): Boolean = {
      httpStatus.isServerError
    }

  }

  // The resource has been exhausted or a per-user quota has reached.
  // The request can be retried after the underlying resource issue is resolved.
  case object RESOURCE_EXHAUSTED extends RPCStatusType {
    override def codeStringPrefix: String = "R"
    override def codeRange: (Int, Int)    = (0x3000, 0x4000)

    override def isValidHttpStatus(httpStatus: HttpStatus): Boolean = {
      httpStatus == HttpStatus.TooManyRequests_429
    }
  }

  def all: Seq[RPCStatusType] = Seq(SUCCESS, USER_ERROR, INTERNAL_ERROR, RESOURCE_EXHAUSTED)

  def unapply(s: String): Option[RPCStatusType] = {
    val name = s.toUpperCase()
    all.find(_.toString == name)
  }
}
