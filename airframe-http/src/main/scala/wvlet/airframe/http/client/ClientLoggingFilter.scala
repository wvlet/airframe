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
import wvlet.airframe.http.RPCMethod
import wvlet.airframe.http.internal.{HttpLogs, RPCCallContext}
import wvlet.log.LogSupport

import java.util.concurrent.TimeUnit
import scala.collection.immutable.ListMap
import scala.concurrent.Future

class ClientLoggingFilter extends ClientFilter with LogSupport {
  override def chain(req: Request, context: ClientContext): Response = {
    val baseTime = System.currentTimeMillis()
    val start    = System.nanoTime()
    val m        = ListMap.newBuilder[String, Any]
    m ++= HttpLogs.unixTimeLogs(baseTime)
    m ++= HttpLogs.commonRequestLogs(req)
    try {
      val resp = context.chain(req)
      m ++= HttpLogs.commonResponseLogs(resp)
      context.getProperty("rpc_method") match {
        case Some(rpcMethod: RPCMethod) =>
          m ++= HttpLogs.rpcMethodLogs(rpcMethod)
        case _ =>
      }
      resp
    } catch {
      case e: Throwable =>
        m ++= HttpLogs.errorLogs(e)
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
