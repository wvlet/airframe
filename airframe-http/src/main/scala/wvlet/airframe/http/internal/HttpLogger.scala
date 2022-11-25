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
package wvlet.airframe.http.internal

import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http._
import wvlet.airframe.surface.{Parameter, Surface, TypeName}
import wvlet.airframe.ulid.ULID
import wvlet.log.LogTimestampFormatter

import java.util.concurrent.ConcurrentHashMap
import scala.annotation.tailrec
import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionException
import scala.util.Try

case class HttpLoggerConfig(
    // TODO
    httpLogWriter: HttpLogWriter = null, // Compat.defaultHttpLogWriter,
    // Additional HTTP headers excluded from logs. Authorization, ProxyAuthorization, Cookie headers will be removed by default
    excludeHeaders: Set[String] = HttpLogger.defaultExcludeHeaders,
    logFilter: Map[String, Any] => Map[String, Any] = identity,
    logFileName: String = "log/http_access.json",
    // The max number of files to preserve in the local disk
    maxNumFiles: Int = 100,
    // The max file size for log rotation. The default is 100MB
    maxFileSize: Long = 100 * 1024 * 1024
) {
  def withHttpLogWriter(httpLogWriter: HttpLogWriter): HttpLoggerConfig = this.copy(httpLogWriter = httpLogWriter)

  /**
    * Add request/response headers to exclude from logging
    */
  def addExcludeHeaders(excludeHeaders: Set[String]): HttpLoggerConfig =
    this.copy(excludeHeaders = this.excludeHeaders ++ excludeHeaders)

  /**
    * Set a file name for log-rotation.
    */
  def withLogFileName(fileName: String): HttpLoggerConfig = this.copy(logFileName = fileName)

  /**
    * Add a filter for customizing log contents before write
    */
  def addLogFilter(newLogFilter: Map[String, Any] => Map[String, Any]): HttpLoggerConfig =
    this.copy(logFilter = logFilter.andThen(newLogFilter))

  def withMaxNumFiles(maxNumFiles: Int): HttpLoggerConfig  = this.copy(maxNumFiles = maxNumFiles)
  def withMaxFileSize(maxFileSize: Long): HttpLoggerConfig = this.copy(maxFileSize = maxFileSize)

  def newClientLogger(name: String): HttpLogger = ???
  def newServerLogger(name: String): HttpLogger = ???
}

object HttpLogger {

  /**
    * Http headers to be excluded from logging by default
    */
  val defaultExcludeHeaders: Set[String] = Set(
    HttpHeader.Authorization,
    HttpHeader.ProxyAuthorization,
    HttpHeader.Cookie
  )

  def unixTimeLogs(currentTimeMillis: Long = System.currentTimeMillis()): ListMap[String, Any] = {
    // Unix time
    ListMap(
      "time"          -> (currentTimeMillis / 1000L),
      "start_time_ms" -> currentTimeMillis,
      // timestamp with ms resolution and zone offset
      "event_time" -> LogTimestampFormatter.formatTimestampWithNoSpaace(currentTimeMillis)
    )
  }

  def commonRequestLogs(request: Request): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    m += "method" -> request.method.toString
    m += "path"   -> request.path
    m += "uri"    -> sanitize(request.uri)
    val queryString = extractQueryString(request.uri)
    if (queryString.nonEmpty) {
      m += "query_string" -> queryString
    }

    request.remoteAddress.foreach { addr =>
      m += "remote_host" -> addr.hostAndPort
      m += "remote_port" -> addr.port
    }

    m ++= requestHeaderLogs(request)
    m.result()
  }

  def commonResponseLogs(response: Response): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    m += "status_code"      -> response.statusCode
    m += "status_code_name" -> response.status.reason
    response.contentLength.foreach {
      m += "response_content_length" -> _
    }
    m ++= responseHeaderLogs(response)
    m.result()
  }

  private def requestHeaderLogs(request: Request): Map[String, Any] = {
    Map("request_header" -> headerLogs(request.header))
  }

  private def responseHeaderLogs(response: Response): Map[String, Any] = {
    Map("response_header" -> headerLogs(response.header))
  }

  private def headerLogs(headerMap: HttpMultiMap, excludeHeaders: Set[String]): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    for (e <- headerMap.entries) {
      if (!excludeHeaders.contains(e.key.toLowerCase)) {
        val v = headerMap.getAll(e.key).mkString(";")
        m += sanitizeHeader(e.key) -> v
      }
    }
    m.result()
  }

  def errorLogs(e: Throwable): ListMap[String, Any] = {

    /**
      * Find the root cause of the exception from wrapped exception classes
      */
    @tailrec
    def findCause(e: Throwable): Throwable = {
      e match {
        case ee: ExecutionException if ee.getCause != null =>
          findCause(ee.getCause)
        // InvocationTargetException is not available in Scala.js, so use the name based lookup
        case i: Exception if i.getClass.getName == "java.lang.reflect.InvocationTargetException" =>
          findCause(i.getCause)
        case _ =>
          e
      }
    }

    val m = ListMap.newBuilder[String, Any]
    // Resolve the cause of the exception
    findCause(e) match {
      case null =>
      // no-op
      case se: HttpServerException =>
        // If the cause is provided, record it. Otherwise, recording the status_code is sufficient.
        if (se.getCause != null) {
          val rootCause = findCause(se.getCause)
          m += "exception"         -> rootCause
          m += "exception_message" -> rootCause.getMessage
        }
      // TODO customize RPC error logs?
      // case re: RPCException =>
      //
      case other =>
        m += "exception"         -> other
        m += "exception_message" -> other.getMessage
    }
    m.result()
  }

  private def sanitize(s: String): String = {
    s.map {
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case c    => c
    }.mkString
  }

  import scala.jdk.CollectionConverters._

  private val headerSanitizeCache = new ConcurrentHashMap[String, String]().asScala

  private def sanitizeHeader(h: String): String = {
    headerSanitizeCache.getOrElseUpdate(h, h.replaceAll("-", "_").toLowerCase())
  }

  def extractQueryString(uri: String): String = {
    val qPos = uri.indexOf('?')
    if (qPos < 0 || qPos == uri.length - 1) {
      ""
    } else {
      uri.substring(qPos + 1, uri.length)
    }
  }

  private def rpcLogs: ListMap[String, Any] = {
    RPCContext.current.getThreadLocal(HttpBackend.TLS_KEY_RPC) match {
      case Some(c: RPCCallContext) =>
        val m = ListMap.newBuilder[String, Any]
        m ++= rpcContext.rpcMethod.logData
        m += "rpc_class" -> rpcContext.rpcClassName

        val rpcArgs = extractRpcArgLog(rpcContext)
        if (rpcArgs.nonEmpty) {
          m += "rpc_args" -> rpcArgs
        }
        m.result()
      case _ =>
        ListMap.empty
    }
  }

  private def extractRpcArgLog(rpcContext: RPCCallContext): ListMap[String, Any] = {

    def traverseObject(s: Surface, arg: Any): ListMap[String, Any] = {
      val builder = ListMap.newBuilder[String, Any]
      s.params
        .foreach { p =>
          Try(builder ++= traverseParam(p, p.get(arg)))
        }
      builder.result()
    }

    def traverseParam(p: Parameter, arg: Any): ListMap[String, Any] = {
      arg match {
        case r: HttpMessage.Request =>
          ListMap.empty
        case r if p.surface.fullName == "com.twitter.finagle.http.Request" =>
          ListMap.empty
        case c: HttpContext[_, _, _] =>
          ListMap.empty
        case _ if p.isSecret =>
          ListMap.empty
        case u: ULID =>
          // Fixes https://github.com/wvlet/airframe/issues/1715
          ListMap(p.name -> u)
        case _ if p.surface.params.length > 0 =>
          ListMap(p.name -> traverseObject(p.surface, arg))
        case _ =>
          ListMap(p.name -> arg)
      }
    }

    val rpcArgsBuilder = ListMap.newBuilder[String, Any]
    // Exclude request context objects, which will be duplicates of request parameter logs
    for ((p, arg) <- rpcContext.rpcMethodSurface.args.zip(rpcContext.rpcArgs)) {
      rpcArgsBuilder ++= traverseParam(p, arg)
    }
    rpcArgsBuilder.result()
  }
}

class HttpLogger(name: String, config: HttpLoggerConfig, writer: HttpLogWriter) {
  import HttpLogger._

  private val excludeHeaders: Set[String] =
    (HttpLogger.defaultExcludeHeaders ++ config.excludeHeaders).map(_.toLowerCase).toSet[String]

  def requestLogs(request: Request): ListMap[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    m ++= unixTimeLogs()
    m ++= commonRequestLogs(request)
    m ++= rpcLogs
    m.result()
  }

  def responseLogs(response: Response): ListMap[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    m ++= commonResponseLogs(response)
  }

}
