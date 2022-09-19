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
package wvlet.airframe.http.finagle.filter
import com.twitter.finagle.http.{HeaderMap, Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import wvlet.airframe.control.MultipleExceptions
import wvlet.airframe.http.finagle.filter.HttpAccessLogFilter._
import wvlet.airframe.http.finagle.{FinagleBackend, FinagleServer}
import wvlet.airframe.http.internal.{HttpLogs, RPCCallContext}
import wvlet.airframe.http.{HttpAccessLogWriter, HttpBackend, HttpHeader, HttpStatus}

import java.util.concurrent.TimeUnit
import scala.collection.immutable.ListMap
import scala.util.Try
import scala.util.control.NonFatal

case class HttpAccessLogFilter(
    httpAccessLogWriter: HttpAccessLogWriter = HttpAccessLogWriter.default,
    // Loggers for request contents
    requestLoggers: Seq[HttpRequestLogger] = HttpAccessLogFilter.defaultRequestLoggers,
    // Loggers for response contents
    responseLoggers: Seq[HttpResponseLogger] = HttpAccessLogFilter.defaultResponseLoggers,
    // Loggers for request errors
    errorLoggers: Seq[HttpErrorLogger] = HttpAccessLogFilter.defaultErrorLoggers,
    // Loggers for thread-local storage contents
    contextLoggers: Seq[HttpContextLogger] = HttpAccessLogFilter.defaultContextLoggers,
    excludeHeaders: Set[String] = Set(HttpHeader.Authorization, HttpHeader.ProxyAuthorization)
) extends SimpleFilter[Request, Response] {

  def addRequestLogger(logger: HttpRequestLogger): HttpAccessLogFilter = {
    this.copy(requestLoggers = requestLoggers :+ logger)
  }
  def addResponseLogger(logger: HttpResponseLogger): HttpAccessLogFilter = {
    this.copy(responseLoggers = responseLoggers :+ logger)
  }
  def addErrorLogger(logger: HttpErrorLogger): HttpAccessLogFilter = {
    this.copy(errorLoggers = errorLoggers :+ logger)
  }
  def addContextLogger(logger: HttpContextLogger): HttpAccessLogFilter = {
    this.copy(contextLoggers = contextLoggers :+ logger)
  }
  def addExcludeHeaders(excludes: Set[String]): HttpAccessLogFilter = {
    this.copy(excludeHeaders = excludeHeaders ++ excludes)
  }

  private val sanitizedExcludeHeaders = excludeHeaders.map(HttpAccessLogWriter.sanitizeHeader)

  private def emit(m: Map[String, Any]) = {
    val filtered = m.filterNot(x => sanitizedExcludeHeaders.contains(x._1))
    httpAccessLogWriter.write(filtered)
  }

  override def apply(request: Request, context: Service[Request, Response]): Future[Response] = {
    // Use ListMap to preserve the parameter order
    val m = ListMap.newBuilder[String, Any]

    val currentNanoTime = System.nanoTime()
    def millisSince     = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - currentNanoTime)

    def report(resp: Option[Response] = None, error: Option[Throwable] = None): Unit = {
      m += "response_time_ms" -> millisSince

      // Report request
      for (l <- requestLoggers) yield Try {
        m ++= l(request)
      }

      // Report response
      resp.foreach { response =>
        for (l <- responseLoggers) {
          m ++= l(response)
        }
      }

      // Report context
      for (l <- contextLoggers) {
        m ++= l(request)
      }

      def reportException(e: Throwable): Unit = {
        for (l <- errorLoggers) {
          m ++= l(request, e)
        }
      }

      // Report a server error that is properly handled at FinagleServer.defaultErrorFilter
      (error, FinagleBackend.getThreadLocal[Throwable](HttpBackend.TLS_KEY_SERVER_EXCEPTION)) match {
        case (Some(e1), Some(e2)) => reportException(MultipleExceptions(Seq(e1, e2)))
        case (Some(e1), None)     => reportException(e1)
        case (None, Some(e2))     => reportException(e2)
        case _                    =>
      }

      emit(m.result())
    }

    def reportError(e: Throwable): Future[Response] = {
      report(error = Some(e))
      Future.exception(e)
    }

    try {
      context(request)
        .map { response =>
          report(resp = Some(response))
          response
        }.rescue { case NonFatal(e: Throwable) =>
          reportError(e)
        }
    } catch {
      // When an unknown internal error happens
      case e: Throwable =>
        reportError(e)
    }
  }
}

object HttpAccessLogFilter {

  def default: HttpAccessLogFilter                        = new HttpAccessLogFilter()
  def traceLoggingFilter: SimpleFilter[Request, Response] = FinagleServer.defaultRequestLogger

  type HttpRequestLogger  = Request => Map[String, Any]
  type HttpResponseLogger = Response => Map[String, Any]
  type HttpErrorLogger    = (Request, Throwable) => Map[String, Any]
  type HttpContextLogger  = Request => Map[String, Any]

  def defaultRequestLoggers: Seq[HttpRequestLogger] =
    Seq(
      unixTimeLogger,
      basicRequestLogger,
      requestHeaderLogger
    )

  def defaultResponseLoggers: Seq[HttpResponseLogger] =
    Seq(
      basicResponseLogger,
      responseHeaderLogger
    )

  def defaultErrorLoggers: Seq[HttpErrorLogger] = Seq(errorLogger)

  def defaultContextLoggers: Seq[HttpContextLogger] = Seq(rpcLogger)

  def unixTimeLogger(request: Request): Map[String, Any] = HttpAccessLogWriter.logUnixTime

  def basicRequestLogger(request: Request): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    m += "method" -> request.method.toString
    m += "path"   -> request.path
    m += "uri"    -> HttpAccessLogWriter.sanitize(request.uri)
    val queryString = extractQueryString(request.uri)
    if (queryString.nonEmpty) {
      m += "query_string" -> queryString
    }
    m += "request_size" -> request.length

    m += "remote_host" -> request.remoteAddress.getHostAddress
    m += "remote_port" -> request.remotePort

    m.result()
  }

  def requestHeaderLogger(request: Request): Map[String, Any] = headerLogger(request.headerMap, None)

  def headerLogger(headerMap: HeaderMap, prefix: Option[String]): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    for ((key, value) <- headerMap) {
      val v = headerMap.getAll(key).mkString(";")
      m += HttpAccessLogWriter.sanitizeHeader(s"${prefix.getOrElse("")}${key}") -> v
    }
    m.result()
  }

  def basicResponseLogger(response: Response): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    m += "status_code"      -> response.statusCode
    m += "status_code_name" -> HttpStatus.ofCode(response.statusCode).reason

    if (response.isChunked) {
      m += "chunked" -> true
    } else {
      m += "response_size" -> response.content.length
    }
    m.result()
  }

  def responseHeaderLogger(response: Response) = headerLogger(response.headerMap, Some("response_"))

  def errorLogger(request: Request, e: Throwable): Map[String, Any] = HttpAccessLogWriter.errorLog(e)
  def rpcLogger(request: Request): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    FinagleBackend.getThreadLocal(HttpBackend.TLS_KEY_RPC).foreach { (x: Any) =>
      x match {
        case c @ RPCCallContext(rpcInterface, methodSurface, args) =>
          m ++= HttpLogs.rpcLogs(c)
        case _ =>
      }
    }
    m.result()
  }

  def extractQueryString(uri: String): String = {
    val qPos = uri.indexOf('?')
    if (qPos < 0 || qPos == uri.length - 1) {
      ""
    } else {
      uri.substring(qPos + 1, uri.length)
    }
  }
}
