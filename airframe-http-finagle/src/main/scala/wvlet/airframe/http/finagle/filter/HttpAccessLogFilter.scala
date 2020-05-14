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
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import com.twitter.finagle.http.{HeaderMap, Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import wvlet.airframe.http.finagle.FinagleServer.findCause
import wvlet.airframe.http.finagle.filter.HttpAccessLogFilter._
import wvlet.airframe.http.finagle.{FinagleBackend, FinagleServer}
import wvlet.airframe.http.{HttpBackend, HttpHeader, HttpServerException, HttpStatus}
import wvlet.airframe.surface.MethodSurface
import wvlet.log.LogTimestampFormatter

import scala.collection.immutable.ListMap
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

  private val sanitizedExcludeHeaders = excludeHeaders.map(sanitizeHeader)

  private def emit(m: Map[String, Any]) = {
    val filtered = m.filterNot(x => sanitizedExcludeHeaders.contains(x._1))
    httpAccessLogWriter.write(filtered)
  }

  override def apply(request: Request, context: Service[Request, Response]): Future[Response] = {
    // Use ListMap to preserve the parameter order
    val m = ListMap.newBuilder[String, Any]
    for (l <- requestLoggers) {
      m ++= l(request)
    }

    val currentNanoTime = System.nanoTime()
    def millisSince     = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - currentNanoTime)

    def reportError(e: Throwable): Unit = {
      m += "response_time_ms" -> millisSince
      reportContext
      for (l <- errorLoggers) {
        m ++= l(request, e)
      }
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

          // Report a server error that is properly handled at FinagleServer.defaultErrorFilter
          FinagleBackend.getThreadLocal(HttpBackend.TLS_KEY_SERVER_EXCEPTION).foreach { x: Any =>
            x match {
              case e: Throwable =>
                reportError(e)
              case _ =>
            }
          }

          emit(m.result())
          response
        }.rescue {
          case NonFatal(e: Throwable) =>
            reportError(e)
            emit(m.result())
            Future.exception(e)
        }
    } catch {
      // When an unknown internal error happens
      case e: Throwable =>
        reportError(e)
        emit(m.result())
        Future.exception(e)
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

  def unixTimeLogger(request: Request): Map[String, Any] = {
    val currentTimeMillis = System.currentTimeMillis()
    // Unix time
    ListMap(
      "time" -> (currentTimeMillis / 1000L),
      // timestamp with ms resolution and zone offset
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

    m.result
  }

  def requestHeaderLogger(request: Request): Map[String, Any] = headerLogger(request.headerMap, None)

  def headerLogger(headerMap: HeaderMap, prefix: Option[String]): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    for ((key, value) <- headerMap) {
      val v = headerMap.getAll(key).mkString(";")
      m += sanitizeHeader(s"${prefix.getOrElse("")}${key}") -> v
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
    m.result
  }

  def responseHeaderLogger(response: Response) = headerLogger(response.headerMap, Some("response_"))

  def errorLogger(request: Request, e: Throwable): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    // Resolve the cause of the exception
    findCause(e) match {
      case null =>
      // no-op
      case se @ HttpServerException(_, cause) =>
        // If the cause is provided, record it. Otherwise, recording the status_code is sufficient.
        if (cause != null) {
          val rootCause = findCause(cause)
          m += "exception"         -> rootCause
          m += "exception_message" -> rootCause.getMessage
        }
      case other =>
        m += "exception"         -> other
        m += "exception_message" -> other.getMessage
    }
    m.result
  }

  def rpcLogger(request: Request): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    FinagleBackend.getThreadLocal(HttpBackend.TLS_KEY_RPC).foreach { x: Any =>
      x match {
        case (methodSurface: MethodSurface, args: Seq[Any]) =>
          m += "rpc_class"  -> methodSurface.owner.fullName
          m += "rpc_method" -> methodSurface.name
          val rpcArgs = (for ((p, arg) <- methodSurface.args.zip(args)) yield {
            p.name -> arg
          }).toMap
          if (rpcArgs.nonEmpty) {
            m += "rpc_args" -> rpcArgs
          }
        case _ =>
      }
    }
    m.result()
  }

  import scala.jdk.CollectionConverters._
  private val headerSanitizeCache = new ConcurrentHashMap[String, String]().asScala

  def sanitizeHeader(h: String): String = {
    headerSanitizeCache.getOrElseUpdate(h, h.replaceAll("-", "_").toLowerCase(Locale.ENGLISH))
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
