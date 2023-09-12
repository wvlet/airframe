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
package wvlet.airframe.http.filter

import wvlet.airframe.http.Http
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.{HttpMessage, HttpMethod, RxHttpEndpoint, RxHttpFilter}
import wvlet.airframe.rx.Rx

import scala.concurrent.duration.Duration

/**
  * Implements https://fetch.spec.whatwg.org/#http-cors-protocol
  */
object Cors {
  case class Policy(
      allowsOrigin: String => Option[String],
      allowsMethods: String => Option[Seq[String]],
      allowsHeaders: Seq[String] => Option[Seq[String]],
      exposedHeaders: Seq[String] = Nil,
      supportsCredentials: Boolean = false,
      maxAge: Option[Duration] = None
  )

  /** A CORS policy that lets you do whatever you want.  Don't use this in production. */
  def unsafePermissivePolicy: Policy = Policy(
    allowsOrigin = origin => Some(origin),
    allowsMethods = method => Some(Seq(method)),
    allowsHeaders = headers => Some(headers),
    supportsCredentials = true
  )

  /**
    * Create a new RxHttpFilter to add headers to support Cross-origin resource sharing (CORS).
    *
    * {{{
    *   Cors.newFilter(
    *     Cors.Policy(
    *       allowsOrigin = origin => { origin match {
    *         case x if x.endsWith("mydomain.com") => Some(origin)
    *         case _ => None
    *       }},
    *       allowsMethods = _ => Some(Seq(HttpMethod.POST)),
    *       allowsHeaders = headers => Some(headers)
    *    ))
    * }}}
    *
    * @param policy
    */
  def newFilter(policy: Policy): RxHttpFilter = new CorsFilter(policy)

  private class CorsFilter(policy: Policy) extends RxHttpFilter {

    private def getOrigin(request: Request): Option[String] =
      request.header.get("Origin")

    private def getMethod(request: Request): Option[String] =
      request.header.get("Access-Control-Request-Method")

    private def commaSpace = ", *".r
    private def getHeaders(request: Request): Seq[String] =
      request.header.get("Access-Control-Request-Headers") match {
        case Some(value) => commaSpace.split(value).toSeq
        case None        => Nil
      }

    private def setOriginAndCredential(resp: Response, origin: String): Response = {
      var r = resp.withHeader("Access-Control-Allow-Origin", origin)
      if (policy.supportsCredentials && origin != "*") {
        r = r.withHeader("Access-Control-Allow-Credentials", "true")
      }
      r
    }

    private def addExposedHeaders(response: Response): Response = {
      if (policy.exposedHeaders.nonEmpty)
        response.withHeader("Access-Control-Expose-Headers", policy.exposedHeaders.mkString(", "))
      else
        response
    }

    private def handlePreflight(request: Request): Option[Response] = {
      getOrigin(request).flatMap { origin =>
        getMethod(request).flatMap { method =>
          val headers = getHeaders(request)
          policy.allowsMethods(method).flatMap { allowedMethods =>
            policy.allowsHeaders(headers).map { allowedHeaders =>
              var resp = Http.response()
              resp = setOriginAndCredential(resp, origin)
              // max-age
              policy.maxAge.foreach { maxAge =>
                resp = resp.withHeader("Access-Control-Max-Age", maxAge.toSeconds.toString)
              }
              // methods
              resp = resp.withHeader("Access-Control-Allow-Methods", allowedMethods.mkString(", "))
              // headers
              resp = resp.withHeader("Access-Control-Allow-Headers", allowedHeaders.mkString(", "))
              resp
            }
          }
        }
      }
    }

    private def handleSimple(request: Request, response: Response): Response = {
      getOrigin(request)
        .map(origin => setOriginAndCredential(response, origin))
        .map(addExposedHeaders)
        .getOrElse(response)
    }

    override def apply(request: HttpMessage.Request, next: RxHttpEndpoint): Rx[HttpMessage.Response] = {
      val resp: Rx[Response] = request.method match {
        case HttpMethod.OPTIONS =>
          // Preflight request
          handlePreflight(request) match {
            case Some(resp) =>
              Rx.single(resp)
            case None =>
              // No matching policy
              Rx.single(Http.response())
          }
        case _ =>
          next(request).map(handleSimple(request, _))
      }
      resp.map { resp =>
        resp.withHeader("Vary", "Origin")
      }
    }
  }
}
