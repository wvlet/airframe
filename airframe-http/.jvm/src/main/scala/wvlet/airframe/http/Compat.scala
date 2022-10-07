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
package wvlet.airframe.http
import wvlet.airframe.control.ThreadUtil

import java.net.URLEncoder
import wvlet.airframe.http.client.{HttpClientBackend, JavaHttpClientBackend, URLConnectionClientBackend}
import wvlet.airframe.http.internal.LocalRPCContext

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}
import scala.concurrent.ExecutionContext

/**
  */
object Compat extends CompatApi {
  override def urlEncode(s: String): String = {
    URLEncoder.encode(s, "UTF-8")
  }
  override def defaultHttpClientBackend: HttpClientBackend = {
    JavaHttpClientBackend
  }

  private val threadFactoryId = new AtomicInteger()

  /**
    * A thread factory for creating a daemon thread so as not to block JVM shutdown
    */
  private class DefaultThreadFactory extends ThreadFactory {
    private val factoryId = threadFactoryId.getAndIncrement()
    private val threadId  = new AtomicInteger()
    override def newThread(r: Runnable): Thread = {
      val threadName = s"airframe-http-${factoryId}:${threadId.getAndIncrement()}"
      val thread     = new Thread(null, r, threadName)
      thread.setName(threadName)
      thread.setDaemon(true)
      thread
    }
  }

  override def defaultExecutionContext: ExecutionContext = {
    // We should not use scala.concurrent.ExecutionContext.global as it might be closed
    ExecutionContext.fromExecutorService(
      Executors.newCachedThreadPool(ThreadUtil.newDaemonThreadFactory("airframe-http"))
    )
  }

  override def hostServerAddress: ServerAddress = {
    // There is no notion of host server in JVM
    ServerAddress.empty
  }

  override def currentRPCContext: RPCContext                     = LocalRPCContext.current
  override def attachRPCContext(context: RPCContext): RPCContext = LocalRPCContext.attach(context)
  override def detachRPCContext(previous: RPCContext): Unit      = LocalRPCContext.detach(previous)
}
