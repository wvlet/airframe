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
import java.util.Locale
import java.util.concurrent.TimeUnit

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import wvlet.airframe.http.HttpStatus
import wvlet.airframe.http.finagle.FinagleServer
import wvlet.airframe.http.finagle.filter.HttpAccessLogFilter.{HttpRequestLogger, _}
import wvlet.log.LogTimestampFormatter

import scala.collection.immutable.ListMap
import scala.util.control.NonFatal

case class HttpAccessLogFilter(
    accessLogWriter: HttpAccessLogWriter = HttpAccessLogWriter.default,
    // Loggers for request contents
    requestLoggers: Seq[HttpRequestLogger] = defaultRequestLoggers,
    // Loggers for response contents
    responseLoggers: Seq[HttpResponseLogger] = defaultResponseLoggers,
    // Loggers for thread-local storage contents
    contextLoggers: Seq[HttpContextLogger] = defaultContextLoggers
) extends SimpleFilter[Request, Response] {

  def addRequestLogger(logger: HttpRequestLogger): HttpAccessLogFilter = {
    this.copy(requestLoggers = requestLoggers :+ logger)
  }
  def addResponseLogger(logger: HttpResponseLogger): HttpAccessLogFilter = {
    this.copy(responseLoggers = responseLoggers :+ logger)
  }
  def aadContextLogger(logger: HttpContextLogger): HttpAccessLogFilter = {
    this.copy(contextLoggers = contextLoggers :+ logger)
  }

  override def apply(request: Request, context: Service[Request, Response]): Future[Response] = {
    // Use ListMap to preserve the parameter order
    val m = ListMap.newBuilder[String, Any]
    for (l <- requestLoggers) {
      m ++= l(request)
    }

    val currentNanoTime = System.nanoTime()
    def millisSince     = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - currentNanoTime)

    def reportError(e: Throwable): Future[Response] = {
      val responseTimeNanos = millisSince
      m += "response_time_ms" -> responseTimeNanos
      reportContext
      m ++= errorLog(request, e)
      accessLogWriter.write(m.result())
      Future.exception(e)
    }

    def reportContext: Unit = {
      for (l <- contextLoggers) {
        m ++= l(request)
      }
    }

    try {
      context(request)
        .map { response =>
          reportContext
          m += "response_time_ms" -> millisSince
          for (l <- responseLoggers) {
            m ++= l(response)
          }
          accessLogWriter.write(m.result())
          response
        }.rescue {
          case NonFatal(e: Throwable) =>
            reportError(e)
        }
    } catch {
      // When an unknown internal error happens
      case e: Throwable =>
        reportError(e)
    }
  }

  private def errorLog(request: Request, e: Throwable): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]

    // Resolve the cause of the exception
    m += "exception" -> FinagleServer.findCause(e)

    m.result
  }
}

object HttpAccessLogFilter {

  def default: HttpAccessLogFilter                        = new HttpAccessLogFilter()
  def traceLoggingFilter: SimpleFilter[Request, Response] = FinagleServer.defaultRequestLogger

  type HttpRequestLogger  = Request => Map[String, Any]
  type HttpResponseLogger = Response => Map[String, Any]
  type HttpContextLogger  = Request => Map[String, Any]

  def defaultRequestLoggers: Seq[HttpRequestLogger] =
    Seq(
      unixTimeLogger,
      basicRequestLogger,
      xHeaderLogger
    )

  def defaultResponseLoggers: Seq[HttpResponseLogger] =
    Seq(
      basicResponseLogger
    )

  def defaultContextLoggers: Seq[HttpContextLogger] = Seq.empty

  def unixTimeLogger(request: Request): Map[String, Any] = {
    val currentTimeMillis = System.currentTimeMillis()
    // Unix time
    ListMap(
      "time" -> (currentTimeMillis / 1000L),
      // Nano-sec timestamp
      "event_time" -> LogTimestampFormatter.formatTimestampWithNoSpaace(currentTimeMillis)
    )
  }

  def basicRequestLogger(request: Request): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    m += "method" -> request.method.toString
    m += "path"   -> request.path
    m += "uri"    -> sanitize(request.uri)
    val queryString = extractQueryString(request.uri)
    if (queryString.nonEmpty) {
      m += "query_string" -> queryString
    }
    m += "request_size" -> request.length

    m += "remote_host" -> request.remoteAddress.getHostAddress
    m += "remote_port" -> request.remotePort
    for (h <- request.host) {
      m += "host" -> h
    }
    if (request.accept.nonEmpty) {
      m += "accept" -> request.accept.mkString(";")
    }
    for (x <- request.userAgent) {
      m += "user_agent" -> x
    }
    m.result
  }

  def xHeaderLogger(request: Request): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    // Record X-XXX headers
    for (xHeader <- request.headerMap.filter(_._1.startsWith("X-")).toSeq) {
      m += sanitizeHeader(xHeader._1) -> xHeader._2
    }
    m.result()
  }

  def basicResponseLogger(response: Response): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    m += "status_code"      -> response.statusCode
    m += "status_code_name" -> HttpStatus.ofCode(response.statusCode).reason
    for (contentLength <- response.contentLength) {
      m += "response_size" -> contentLength
    }

    // Record X-XXX headers in the response
    for (xHeader <- response.headerMap.filter(_._1.startsWith("X-"))) {
      m += s"response_${sanitizeHeader(xHeader._1)}" -> xHeader._2
    }
    m.result
  }

  def sanitizeHeader(h: String): String = {
    h.replaceAll("-", "_").toLowerCase(Locale.ENGLISH)
  }

  def sanitize(s: String): String = {
    s.map {
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case c    => c
    }.mkString
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
