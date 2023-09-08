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
import wvlet.airframe.http.*
import wvlet.airframe.http.client.HttpClientContext
import wvlet.airframe.rx.Rx
import wvlet.airframe.surface.{Parameter, Surface, TypeName}
import wvlet.airframe.ulid.ULID
import wvlet.log.LogTimestampFormatter

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import scala.annotation.tailrec
import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionException
import scala.util.Try

/**
  * Internal utilities for HTTP request/response logging
  */
object HttpLogs {

  def reportLog(
      httpLogger: HttpLogger,
      excludeHeaders: HttpMultiMap,
      request: Request,
      next: RxHttpEndpoint,
      clientContext: Option[HttpClientContext] = None,
      rpcContext: Option[RPCContext] = None
  ): Rx[Response] = {
    val baseTime = System.currentTimeMillis()
    val start    = System.nanoTime()
    val m        = ListMap.newBuilder[String, Any]
    m ++= unixTimeLogs(baseTime)
    m ++= commonRequestLogs(request)
    m ++= requestHeaderLogs(request, excludeHeaders)

    def reportLogs: Unit = {
      // Finally, write the log
      httpLogger.write(httpLogger.config.logFilter(m.result()))
    }

    def rpcCallLogs(): Unit = {
      // RPC call context will be set to a TLS after dispatching an event
      // TODO Pass RPC call context without using TLS
      rpcContext.flatMap(_.rpcCallContext).foreach { rcc =>
        m ++= rpcLogs(rcc)
      }
    }

    clientContext.foreach {
      _.rpcMethod.map { rpc => m ++= rpcMethodLogs(rpc) }
    }

    next
      .apply(request)
      .toRx
      .map { resp =>
        m ++= durationLogs(baseTime, start)
        rpcCallLogs()
        m ++= commonResponseLogs(resp)
        m ++= responseHeaderLogs(resp, excludeHeaders)
        reportLogs
        resp
      }
      .recoverWith { case e: Throwable =>
        m ++= durationLogs(baseTime, start)
        rpcCallLogs()
        m ++= errorLogs(e)
        reportLogs
        Rx.exception(e)
      }
  }

  def durationLogs(baseTime: Long, sinceNano: Long): ListMap[String, Any] = {
    val end           = System.nanoTime()
    val durationMills = TimeUnit.NANOSECONDS.toMillis(end - sinceNano)
    ListMap(
      "end_time_ms" -> (baseTime + durationMills),
      "duration_ms" -> durationMills
    )
  }

  def unixTimeLogs(currentTimeMillis: Long = System.currentTimeMillis()): ListMap[String, Any] = {
    // Unix time
    ListMap(
      "time"          -> (currentTimeMillis / 1000L),
      "start_time_ms" -> currentTimeMillis,
      // timestamp with ms resolution and zone offset
      "event_timestamp" -> LogTimestampFormatter.formatTimestampWithNoSpaace(currentTimeMillis)
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
    request.remoteAddress.foreach { remoteAddr =>
      m += "remote_address" -> remoteAddr.hostAndPort
    }
    m.result()
  }

  def commonResponseLogs(response: Response): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    m += "status_code"      -> response.statusCode
    m += "status_code_name" -> response.status.reason
    response.contentLength.foreach {
      m += "response_content_length" -> _
    }

    response.getHeader(HttpHeader.xAirframeRPCStatus).foreach { rpcStatus =>
      Try(RPCStatus.ofCode(rpcStatus.toInt)).foreach { status =>
        m ++= rpcStatusLogs(status)
      }
    }
    m.result()
  }

  def rpcStatusLogs(status: RPCStatus): Map[String, Any] = {
    ListMap(
      "rpc_status"      -> status.code,
      "rpc_status_name" -> status.name
    )
  }

  def requestHeaderLogs(request: Request, excludeHeaders: HttpMultiMap): Map[String, Any] = {
    val m = headerLogs(request.header, excludeHeaders)
    if (m.isEmpty)
      Map.empty
    else
      Map("request_header" -> m)
  }

  def responseHeaderLogs(response: Response, excludeHeaders: HttpMultiMap): Map[String, Any] = {
    val m = headerLogs(response.header, excludeHeaders)
    if (m.isEmpty)
      Map.empty
    else
      Map("response_header" -> m)
  }

  def headerLogs(headerMap: HttpMultiMap, excludeHeaders: HttpMultiMap): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    for (e <- headerMap.entries) {
      if (!excludeHeaders.contains(e.key.toLowerCase)) {
        val v = headerMap.getAll(e.key).mkString(";")
        m += sanitizeHeader(e.key) -> v
      }
    }
    m.result()
  }

  def rpcMethodLogs(rpcMethod: RPCMethod): Map[String, Any] = {
    rpcMethod.logData
  }

  def rpcLogs(rpcContext: RPCCallContext): ListMap[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    m += "rpc_interface" -> rpcContext.rpcInterfaceName
    m += "rpc_class"     -> rpcContext.rpcClassName
    m += "rpc_method"    -> rpcContext.rpcMethodName

    val rpcArgs = extractRpcArgLog(rpcContext)
    if (rpcArgs.nonEmpty) {
      m += "rpc_args" -> rpcArgs
    }
    m.result()
  }

  private[http] def extractRpcArgLog(rpcContext: RPCCallContext): ListMap[String, Any] = {

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

  private[http] def errorLogs(e: Throwable): ListMap[String, Any] = {

    /**
      * Find the root cause of the exception from wrapped exception classes
      */
    @tailrec
    def findCause(e: Throwable): Throwable = {
      e match {
        // InvocationTargetException is not available in Scala.js
        case i: Exception if i.getClass.getName == "java.lang.reflect.InvocationTargetException" =>
          findCause(i.getCause)
        case ee: ExecutionException if ee.getCause != null =>
          findCause(ee.getCause)
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
          m += "exception_message" -> rootCause.getMessage
          m += "exception"         -> rootCause
        }
      case re: RPCException =>
        // Customize RPC error logs
        m ++= rpcStatusLogs(re.status)
        m += "exception_message" -> re.getMessage
        if (re.shouldReportStackTrace) {
          m += "exception" -> re
        }
      case other =>
        m += "exception_message" -> other.getMessage
        m += "exception"         -> other
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

  import scala.jdk.CollectionConverters.*
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

}
