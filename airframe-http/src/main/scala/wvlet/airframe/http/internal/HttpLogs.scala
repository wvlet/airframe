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

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import scala.annotation.tailrec
import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionException
import scala.util.Try

object HttpLogs {

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

  def requestHeaderLogs(request: Request): Map[String, Any] = {
    Map("request_header" -> headerLogs(request.header))
  }

  def responseHeaderLogs(response: Response): Map[String, Any] = {
    Map("response_header" -> headerLogs(response.header))
  }

  /**
    * Http headers to be excluded from logging by dfeault
    */
  private val defaultExcludeHeaders: Set[String] = Set(
    HttpHeader.Authorization,
    HttpHeader.Cookie
  ).map(_.toLowerCase)

  def headerLogs(headerMap: HttpMultiMap): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    for (e <- headerMap.entries) {
      if (!defaultExcludeHeaders.contains(e.key.toLowerCase)) {
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
    m += "rpc_interface" -> TypeName.sanitizeTypeName(rpcContext.rpcInterfaceCls.getName)
    m += "rpc_class"     -> rpcContext.rpcMethodSurface.owner.fullName
    m += "rpc_method"    -> rpcContext.rpcMethodSurface.name

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

}
