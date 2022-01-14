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
import wvlet.airframe.msgpack.spi.Value.{IntegerValue, LongValue, StringValue}
import wvlet.airframe.msgpack.spi.{Packer, Unpacker}

import scala.util.{Failure, Success, Try}

class HttpStatus(val code: Int) extends PackSupport {
  override def toString = s"[${code}: ${reason}]"
  override def pack(p: Packer): Unit = {
    p.packInt(code)
  }
  def reason: String =
    HttpStatus.reasons.get(this) match {
      case Some(reason)    => reason
      case _ if code < 100 => "Unknown Status"
      case _ if code < 200 => "Informational"
      case _ if code < 300 => "Successful"
      case _ if code < 400 => "Redirection"
      case _ if code < 500 => "Client Error"
      case _ if code < 600 => "Server Error"
      case _               => "Unknown Status"
    }

  def isUnknownState: Boolean  = HttpStatus.isUnknownState(code)
  def isInformational: Boolean = HttpStatus.isInformational(code)
  def isSuccessful: Boolean    = HttpStatus.isSuccessful(code)
  def isRedirection: Boolean   = HttpStatus.isRedirection(code)
  def isClientError: Boolean   = HttpStatus.isClientError(code)
  def isServerError: Boolean   = HttpStatus.isServerError(code)
}

/**
  * HTTP status code collection.
  */
object HttpStatus {
  def isUnknownState(code: Int): Boolean  = code < 100 || code >= 600
  def isInformational(code: Int): Boolean = 100 <= code && code < 200
  def isSuccessful(code: Int): Boolean    = 200 <= code && code < 300
  def isRedirection(code: Int): Boolean   = 300 <= code && code < 400
  def isClientError(code: Int): Boolean   = 400 <= code && code < 500
  def isServerError(code: Int): Boolean   = 500 <= code && code < 600

  def ofCode(code: Int): HttpStatus = {
    statusTable.getOrElse(code, new HttpStatus(code))
  }

  def unapply(s: String): Option[HttpStatus] = {
    Try(s.toInt) match {
      case Success(code) => statusTable.get(code)
      case Failure(e)    => None
    }
  }

  def unpack(u: Unpacker): Option[HttpStatus] = {
    u.unpackValue match {
      case l @ LongValue(v) if l.isValidInt => statusTable.get(v.toInt)
      case StringValue(s)                   => Try(s.toInt).toOption.flatMap(statusTable.get(_))
      case other                            => None
    }
  }

  // Unknown status
  case object Unknown_000                       extends HttpStatus(0)
  case object Continue_100                      extends HttpStatus(100)
  case object SwitchingProtocols_101            extends HttpStatus(101)
  case object Processing_102                    extends HttpStatus(102)
  case object Ok_200                            extends HttpStatus(200)
  case object Created_201                       extends HttpStatus(201)
  case object Accepted_202                      extends HttpStatus(202)
  case object NonAuthoritativeInformation_203   extends HttpStatus(203)
  case object NoContent_204                     extends HttpStatus(204)
  case object ResetContent_205                  extends HttpStatus(205)
  case object PartialContent_206                extends HttpStatus(206)
  case object MultiStatus_207                   extends HttpStatus(207)
  case object MultipleChoices_300               extends HttpStatus(300)
  case object MovedPermanently_301              extends HttpStatus(301)
  case object Found_302                         extends HttpStatus(302)
  case object SeeOther_303                      extends HttpStatus(303)
  case object NotModified_304                   extends HttpStatus(304)
  case object UseProxy_305                      extends HttpStatus(305)
  case object TemporaryRedirect_307             extends HttpStatus(307)
  case object PermanentRedirect_308             extends HttpStatus(308)
  case object BadRequest_400                    extends HttpStatus(400)
  case object Unauthorized_401                  extends HttpStatus(401)
  case object PaymentRequired_402               extends HttpStatus(402)
  case object Forbidden_403                     extends HttpStatus(403)
  case object NotFound_404                      extends HttpStatus(404)
  case object MethodNotAllowed_405              extends HttpStatus(405)
  case object NotAcceptable_406                 extends HttpStatus(406)
  case object ProxyAuthenticationRequired_407   extends HttpStatus(407)
  case object RequestTimeout_408                extends HttpStatus(408)
  case object Conflict_409                      extends HttpStatus(409)
  case object Gone_410                          extends HttpStatus(410)
  case object LengthRequired_411                extends HttpStatus(411)
  case object PreconditionFailed_412            extends HttpStatus(412)
  case object RequestEntityTooLarge_413         extends HttpStatus(413)
  case object RequestURITooLong_414             extends HttpStatus(414)
  case object UnsupportedMediaType_415          extends HttpStatus(415)
  case object RequestedRangeNotSatisfiable_416  extends HttpStatus(416)
  case object ExpectationFailed_417             extends HttpStatus(417)
  case object EnhanceYourCalm_420               extends HttpStatus(420)
  case object UnprocessableEntity_422           extends HttpStatus(422)
  case object Locked_423                        extends HttpStatus(423)
  case object FailedDependency_424              extends HttpStatus(424)
  case object UnorderedCollection_425           extends HttpStatus(425)
  case object UpgradeRequired_426               extends HttpStatus(426)
  case object PreconditionRequired_428          extends HttpStatus(428)
  case object TooManyRequests_429               extends HttpStatus(429)
  case object RequestHeaderFieldsTooLarge_431   extends HttpStatus(431)
  case object UnavailableForLegalReasons_451    extends HttpStatus(451)
  case object ClientClosedRequest_499           extends HttpStatus(499)
  case object InternalServerError_500           extends HttpStatus(500)
  case object NotImplemented_501                extends HttpStatus(501)
  case object BadGateway_502                    extends HttpStatus(502)
  case object ServiceUnavailable_503            extends HttpStatus(503)
  case object GatewayTimeout_504                extends HttpStatus(504)
  case object HttpVersionNotSupported_505       extends HttpStatus(505)
  case object VariantAlsoNegotiates_506         extends HttpStatus(506)
  case object InsufficientStorage_507           extends HttpStatus(507)
  case object NotExtended_510                   extends HttpStatus(510)
  case object NetworkAuthenticationRequired_511 extends HttpStatus(511)

  private val reasons: Map[HttpStatus, String] = Map(
    Unknown_000                       -> "Unknown",
    Continue_100                      -> "Continue",
    SwitchingProtocols_101            -> "Switching Protocols",
    Processing_102                    -> "Processing",
    Ok_200                            -> "OK",
    Created_201                       -> "Created",
    Accepted_202                      -> "Accepted",
    NonAuthoritativeInformation_203   -> "Non-Authoritative Information",
    NoContent_204                     -> "No Content",
    ResetContent_205                  -> "Reset Content",
    PartialContent_206                -> "Partial Content",
    MultiStatus_207                   -> "Multi-Status",
    MultipleChoices_300               -> "Multiple Choices",
    MovedPermanently_301              -> "Moved Permanently",
    Found_302                         -> "Found",
    SeeOther_303                      -> "See Other",
    NotModified_304                   -> "Not Modified",
    UseProxy_305                      -> "Use Proxy",
    TemporaryRedirect_307             -> "Temporary Redirect",
    PermanentRedirect_308             -> "Permanent Redirect",
    BadRequest_400                    -> "Bad Request",
    Unauthorized_401                  -> "Unauthorized",
    PaymentRequired_402               -> "Payment Required",
    Forbidden_403                     -> "Forbidden",
    NotFound_404                      -> "Not Found",
    MethodNotAllowed_405              -> "Method Not Allowed",
    NotAcceptable_406                 -> "Not Acceptable",
    ProxyAuthenticationRequired_407   -> "Proxy Authentication Required",
    RequestTimeout_408                -> "Request Timeout",
    Conflict_409                      -> "Conflict",
    Gone_410                          -> "Gone",
    LengthRequired_411                -> "Length Required",
    PreconditionFailed_412            -> "Precondition Failed",
    RequestEntityTooLarge_413         -> "Request Entity Too Large",
    RequestURITooLong_414             -> "Request-URI Too Long",
    UnsupportedMediaType_415          -> "Unsupported Media Type",
    RequestedRangeNotSatisfiable_416  -> "Requested Range Not Satisfiable",
    ExpectationFailed_417             -> "Expectation Failed",
    EnhanceYourCalm_420               -> "Enhance Your Calm",
    UnprocessableEntity_422           -> "Unprocessable Entity",
    Locked_423                        -> "Locked",
    FailedDependency_424              -> "Failed Dependency",
    UnorderedCollection_425           -> "Unordered Collection",
    UpgradeRequired_426               -> "Upgrade Required",
    PreconditionRequired_428          -> "Precondition Required",
    TooManyRequests_429               -> "Too Many Requests",
    RequestHeaderFieldsTooLarge_431   -> "Request Header Fields Too Large",
    UnavailableForLegalReasons_451    -> "Unavailable For Legal Reasons",
    ClientClosedRequest_499           -> "Client Closed Request",
    InternalServerError_500           -> "Internal Server Error",
    NotImplemented_501                -> "Not Implemented",
    BadGateway_502                    -> "Bad Gateway",
    ServiceUnavailable_503            -> "Service Unavailable",
    GatewayTimeout_504                -> "Gateway Timeout",
    HttpVersionNotSupported_505       -> "HTTP Version Not Supported",
    VariantAlsoNegotiates_506         -> "Variant Also Negotiates",
    InsufficientStorage_507           -> "Insufficient Storage",
    NotExtended_510                   -> "Not Extended",
    NetworkAuthenticationRequired_511 -> "Network Authentication Required"
  )

  def knownStatuses: Seq[HttpStatus]            = reasons.keys.toSeq
  private val statusTable: Map[Int, HttpStatus] = knownStatuses.map(x => x.code -> x).toMap
}
