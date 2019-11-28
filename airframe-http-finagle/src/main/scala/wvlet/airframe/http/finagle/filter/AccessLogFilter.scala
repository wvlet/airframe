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

import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import wvlet.airframe.http.{HttpContext, HttpFilter}
import wvlet.airframe.http.finagle.FinagleFilter
import wvlet.airframe._

import scala.util.control.NonFatal

trait AccessLogger {
  def emit(log: Map[String, Any])
}

/**
  *
  */
case class AccessLogFilter(
    accessLogger: AccessLogger,
    requestLog: Request => Map[String, Any],
    responseLog: (Request, Response) => Map[String, Any],
    errorLog: (Request, Throwable) => Map[String, Any]
) extends FinagleFilter {
  override def apply(request: Request, context: HttpContext[Request, Response, Future]): Future[Response] = {
    val m = Map.newBuilder[String, Any]
    m ++= requestLog(request)
    val currentNanoTime = System.nanoTime()

    def nanosSince = System.nanoTime() - currentNanoTime

    def reportError(e: Throwable): Future[Response] = {
      val responseTimeNanos = nanosSince
      m += "response_time_ns" -> responseTimeNanos
      m ++= errorLog(request, e)
      accessLogger.emit(m.result())
      Future.exception(e)
    }
    try {
      // Prepare a placeholder to store the current user's account_id and user_id.
      // This holder will be set by AuthenticationFilter or DataSetsAPI
      context.withContextParam {
        context(request)
          .map { response =>
            m += "response_time_ns" -> nanosSince
            m ++= responseLog(request, response)
            accessLogger.emit(m.result())
            response
          }.rescue {
            case NonFatal(e: Throwable) =>
              reportError(e)
          }
      }
    } catch {
      // When an unknown internal error happens
      case e: Throwable =>
        reportError(e)
    }
  }
}

object AccessLogFilter {}
