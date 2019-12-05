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

import java.util.concurrent.TimeUnit

import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import wvlet.airframe.http.HttpContext
import wvlet.airframe.http.finagle.FinagleFilter

import scala.util.control.NonFatal

trait HttpAccessLogOutput {
  def emit(log: Map[String, Any])
}

/**
  *
  */
case class HttpAccessLogger(
    logOutput: HttpAccessLogOutput,
    requestLog: Request => Map[String, Any],
    responseLog: (Request, Response) => Map[String, Any],
    errorLog: (Request, Throwable) => Map[String, Any]
) extends FinagleFilter {
  override def apply(request: Request, context: HttpContext[Request, Response, Future]): Future[Response] = {
    val m = Map.newBuilder[String, Any]
    m ++= requestLog(request)
    val currentNanoTime = System.nanoTime()

    def millisSince = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - currentNanoTime)

    def reportError(e: Throwable): Future[Response] = {
      val responseTimeMillis = millisSince
      m += "response_time_ms" -> responseTimeMillis
      m ++= errorLog(request, e)
      logOutput.emit(m.result())
      Future.exception(e)
    }
    try {
      context(request)
        .map { response =>
          m += "response_time_ms" -> millisSince
          m ++= responseLog(request, response)
          logOutput.emit(m.result())
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
}

object HttpAccessLogger {}
