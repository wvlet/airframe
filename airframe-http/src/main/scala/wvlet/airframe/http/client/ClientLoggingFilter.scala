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
package wvlet.airframe.http.client
import wvlet.airframe.http.{HttpMessage, HttpMultiMap}
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.log.{LogSupport, LogTimestampFormatter}

import java.util.Locale
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import scala.collection.immutable.ListMap
import scala.concurrent.Future

class ClientLoggingFilter extends ClientFilter with LogSupport {
  override def chain(req: Request, context: ClientContext): Response = {
    val baseTime = System.currentTimeMillis()
    val start    = System.nanoTime()
    val m        = ListMap.newBuilder[String, Any]
    m ++= ClientLogs.unixTimeLogs(baseTime)
    m ++= ClientLogs.commonRequestLogs(req)
    try {
      val resp = context.chain(req)
      m ++= ClientLogs.commonResponseLogs(resp)
      resp
    } catch {
      case e: Throwable =>
        warn(e)
        throw e
    } finally {
      val end           = System.nanoTime()
      val durationMills = TimeUnit.NANOSECONDS.toMillis(end - start)
      m += "duration_ms" -> durationMills
      m += "end_time_ms" -> (baseTime + durationMills)
      trace(m.result())
    }
  }

  override def chainAsync(req: Request, context: ClientContext): Future[Response] = {
    context.chainAsync(req)
  }
}

object ClientLogs {

  def unixTimeLogs(currentTimeMillis: Long): ListMap[String, Any] = {
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

  def headerLogs(headerMap: HttpMultiMap): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    for (e <- headerMap.entries) {
      val v = headerMap.getAll(e.key).mkString(";")
      m += sanitizeHeader(e.key) -> v
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
    headerSanitizeCache.getOrElseUpdate(h, h.replaceAll("-", "_").toLowerCase(Locale.ENGLISH))
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
