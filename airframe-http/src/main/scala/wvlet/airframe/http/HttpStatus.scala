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

class HttpStatus(val code: Int, val message: String) {
  def isUnknownState: Boolean  = code < 100 || code >= 600
  def isInformational: Boolean = 100 <= code && code < 200
  def isSuccessful: Boolean    = 200 <= code && code < 300
  def isRedirection: Boolean   = 300 <= code && code < 400
  def isClientError: Boolean   = 400 <= code && code < 500
  def isServerError: Boolean   = 500 <= code && code < 600
}

/**
  *
  */
object HttpStatus {

  def ofCode(code: Int): HttpStatus = {
    statusTable.getOrElse(code, HttpStatus(code, s"Status ${code}"))
  }

  case object Continue                      extends HttpStatus(100, "Continue")
  case object SwitchingProtocols            extends HttpStatus(101, "Switching Protocols")
  case object Processing                    extends HttpStatus(102, "Processing")
  case object Ok                            extends HttpStatus(200, "Ok")
  case object Created                       extends HttpStatus(201, "Created")
  case object Accepted                      extends HttpStatus(202, "Accepted")
  case object NonAuthoritativeInformation   extends HttpStatus(203, "Non-Authoritative Information")
  case object NoContent                     extends HttpStatus(204, "No Content")
  case object ResetContent                  extends HttpStatus(205, "Reset Content")
  case object PartialContent                extends HttpStatus(206, "Partial Content")
  case object MultiStatus                   extends HttpStatus(207, "Multi-Status")
  case object MultipleChoices               extends HttpStatus(300, "Multiple Choices")
  case object MovedPermanently              extends HttpStatus(301, "Moved Permanentaly")
  case object Found                         extends HttpStatus(302, "Found")
  case object SeeOther                      extends HttpStatus(303, "See Other")
  case object NotModified                   extends HttpStatus(304, "Not Modified")
  case object UseProxy                      extends HttpStatus(305, "Use Proxy")
  case object TemporaryRedirect             extends HttpStatus(307, "Temporary Rediect")
  case object PermanentRedirect             extends HttpStatus(308, "Permanent Redirect")
  case object BadRequest                    extends HttpStatus(400, "Bad Request")
  case object Unauthorized                  extends HttpStatus(401, "Unauthorized")
  case object PaymentRequired               extends HttpStatus(402, "Payment Required")
  case object Forbidden                     extends HttpStatus(403, "Forbidden")
  case object NotFound                      extends HttpStatus(404, "Not Found")
  case object MethodNotAllowed              extends HttpStatus(405, "Method Not Allowed")
  case object NotAcceptable                 extends HttpStatus(406, "Not Acceptable")
  case object ProxyAuthenticationRequired   extends HttpStatus(407, "Proxy Authentication Required")
  case object RequestTimeout                extends HttpStatus(408, "Request Timeout")
  case object Conflict                      extends HttpStatus(409, "Conflict")
  case object Gone                          extends HttpStatus(410, "Gone")
  case object LengthRequired                extends HttpStatus(411, "Length Requried")
  case object PreconditionFailed            extends HttpStatus(412, "Precondition Failed")
  case object RequestEntityTooLarge         extends HttpStatus(413, "Request Entity Too Large")
  case object RequestURITooLong             extends HttpStatus(414, "Request URI Too Long")
  case object UnsupportedMediaType          extends HttpStatus(415, "Unsupported Media Type")
  case object RequestedRangeNotSatisfiable  extends HttpStatus(416, "Request Range Not Satisfiable")
  case object ExpectationFailed             extends HttpStatus(417, "Expectation Failed")
  case object EnhanceYourCalm               extends HttpStatus(420, "Enhance Your Calm")
  case object UnprocessableEntity           extends HttpStatus(422, "Unprocessable Entity")
  case object Locked                        extends HttpStatus(423, "Locked")
  case object FailedDependency              extends HttpStatus(424, "Failed Dependency")
  case object UnorderedCollection           extends HttpStatus(425, "Unordered Collection")
  case object UpgradeRequired               extends HttpStatus(426, "Upgrade Required")
  case object PreconditionRequired          extends HttpStatus(428, "Precondition Required")
  case object TooManyRequests               extends HttpStatus(429, "Too Many Requests")
  case object RequestHeaderFieldsTooLarge   extends HttpStatus(431, "Request Header Fields Too Large")
  case object UnavailableForLegalReasons    extends HttpStatus(451, "Unavailable For Legal Reasons")
  case object ClientClosedRequest           extends HttpStatus(499, "Client Closed Request")
  case object InternalServerError           extends HttpStatus(500, "Internal Server Error")
  case object NotImplemented                extends HttpStatus(501, "Not Implemented")
  case object BadGateway                    extends HttpStatus(502, "Bad Gateway")
  case object ServiceUnavailable            extends HttpStatus(503, "Service Unavailable")
  case object GatewayTimeout                extends HttpStatus(504, "Gateway Timeout")
  case object HttpVersionNotSupported       extends HttpStatus(505, "HTTP Version Not Suported")
  case object VariantAlsoNegotiates         extends HttpStatus(506, "Variant Also Negotiates")
  case object InsufficientStorage           extends HttpStatus(507, "Insufficient Storage")
  case object NotExtended                   extends HttpStatus(510, "Not Extended")
  case object NetworkAuthenticationRequired extends HttpStatus(511, "Network Authentication Required")

  val knownStatuses = Seq(
    Continue,
    SwitchingProtocols,
    Processing,
    Ok,
    Created,
    Accepted,
    NonAuthoritativeInformation,
    NoContent,
    ResetContent,
    PartialContent,
    MultiStatus,
    MultipleChoices,
    MovedPermanently,
    Found,
    SeeOther,
    NotModified,
    UseProxy,
    TemporaryRedirect,
    PermanentRedirect,
    BadRequest,
    Unauthorized,
    PaymentRequired,
    Forbidden,
    NotFound,
    MethodNotAllowed,
    NotAcceptable,
    ProxyAuthenticationRequired,
    RequestTimeout,
    Conflict,
    Gone,
    LengthRequired,
    PreconditionFailed,
    RequestEntityTooLarge,
    RequestURITooLong,
    UnsupportedMediaType,
    RequestedRangeNotSatisfiable,
    ExpectationFailed,
    EnhanceYourCalm,
    UnprocessableEntity,
    Locked,
    FailedDependency,
    UnorderedCollection,
    UpgradeRequired,
    PreconditionRequired,
    TooManyRequests,
    RequestHeaderFieldsTooLarge,
    UnavailableForLegalReasons,
    ClientClosedRequest,
    InternalServerError,
    NotImplemented,
    BadGateway,
    ServiceUnavailable,
    GatewayTimeout,
    HttpVersionNotSupported,
    VariantAlsoNegotiates,
    InsufficientStorage,
    NotExtended,
    NetworkAuthenticationRequired
  )

  private val statusTable: Map[Int, HttpStatus] = knownStatuses.map(x => x.code -> x).toMap
}
