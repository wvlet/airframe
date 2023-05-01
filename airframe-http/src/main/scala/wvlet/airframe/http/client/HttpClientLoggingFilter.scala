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
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.internal.HttpLogs
import wvlet.airframe.http.{RxHttpEndpoint, RxHttpFilter}
import wvlet.airframe.rx.Rx
import wvlet.log.LogSupport

import java.util.concurrent.TimeUnit
import scala.collection.immutable.ListMap

/**
  * A client-side filter for logging HTTP requests and responses
  */
object HttpClientLoggingFilter extends HttpClientFilter with LogSupport {
  def apply(context: HttpClientContext): RxHttpFilter = new RxHttpFilter {
    override def apply(request: Request, next: RxHttpEndpoint): Rx[Response] = {
      val baseTime = System.currentTimeMillis()
      val start    = System.nanoTime()
      val m        = ListMap.newBuilder[String, Any]
      m += "client_name" -> context.clientName
      m ++= HttpLogs.unixTimeLogs(baseTime)
      m ++= HttpLogs.commonRequestLogs(request)

      def recordDuration: Unit = {
        val end           = System.nanoTime()
        val durationMills = TimeUnit.NANOSECONDS.toMillis(end - start)
        m += "duration_ms" -> durationMills
        m += "end_time_ms" -> (baseTime + durationMills)
        logger.trace(m.result())
      }

      context.rpcMethod.map { rpc =>
        m ++= HttpLogs.rpcMethodLogs(rpc)
      }
      // TODO Record rpc args
      next
        .apply(request)
        .toRxStream
        .map { resp =>
          m ++= HttpLogs.commonResponseLogs(resp)
          context.rpcMethod.foreach { rpcMethod =>
            m ++= HttpLogs.rpcMethodLogs(rpcMethod)
          }
          recordDuration
          resp
        }
        .recoverWith { case e: Throwable =>
          m ++= HttpLogs.errorLogs(e)
          recordDuration
          Rx.exception(e)
        }
    }
  }
}

