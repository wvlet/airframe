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
import wvlet.airframe.control.ResultClass.{Failed, nonRetryableFailure, retryableFailure}
import wvlet.airframe.control.ThreadUtil
import wvlet.airframe.http.client.{HttpClientBackend, JavaHttpClientBackend}
import wvlet.airframe.http.internal.{LocalRPCContext, LogRotationHttpLogger}

import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.*
import java.nio.channels.ClosedChannelException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutionException, Executors, ThreadFactory}
import javax.net.ssl.{SSLException, SSLHandshakeException, SSLKeyException, SSLPeerUnverifiedException}
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
  override def defaultHttpClientLoggerFactory: HttpLoggerConfig => HttpLogger = { (config: HttpLoggerConfig) =>
    new LogRotationHttpLogger(config)
  }

  override def hostServerAddress: ServerAddress = {
    // There is no notion of host server in JVM
    ServerAddress.empty
  }

  override def currentRPCContext: RPCContext                     = LocalRPCContext.current
  override def attachRPCContext(context: RPCContext): RPCContext = LocalRPCContext.attach(context)
  override def detachRPCContext(previous: RPCContext): Unit      = LocalRPCContext.detach(previous)

  /**
    * SSL exception classifier for JVM platform. This handles SSL-specific exceptions that are only available on JVM.
    */
  override def sslExceptionClassifier: PartialFunction[Throwable, Failed] = { case e: SSLException =>
    e match {
      // Deterministic SSL exceptions are not retryable
      case se: SSLHandshakeException      => nonRetryableFailure(e)
      case se: SSLKeyException            => nonRetryableFailure(e)
      case s3: SSLPeerUnverifiedException => nonRetryableFailure(e)
      case other                          =>
        // SSLProtocolException and uncategorized SSL exceptions (SSLException) such as unexpected_message may be retryable
        retryableFailure(e)
    }
  }

  /**
    * Connection exception classifier for JVM platform. This handles java.net exceptions.
    */
  override def connectionExceptionClassifier: PartialFunction[Throwable, Failed] = {
    // Other types of exception that can happen inside HTTP clients (e.g., Jetty)
    case e: java.lang.InterruptedException =>
      // Retryable when the http client thread execution is interrupted.
      retryableFailure(e)
    case e: ProtocolException      => retryableFailure(e)
    case e: ConnectException       => retryableFailure(e)
    case e: ClosedChannelException => retryableFailure(e)
    case e: SocketTimeoutException => retryableFailure(e)
    case e: SocketException =>
      e match {
        case se: BindException            => retryableFailure(e)
        case se: ConnectException         => retryableFailure(e)
        case se: NoRouteToHostException   => retryableFailure(e)
        case se: PortUnreachableException => retryableFailure(e)
        case se if se.getMessage() == "Socket closed" => retryableFailure(e)
        case other =>
          nonRetryableFailure(e)
      }
    // HTTP/2 may disconnects the connection with "GOAWAY received" error https://github.com/wvlet/airframe/issues/3421
    // See also the code of jdk.internal.net.http.Http2Connection.handleGoAway
    case e: IOException if Option(e.getMessage()).exists(_.contains("GOAWAY received")) =>
      retryableFailure(e)
    // Exceptions from Finagle. Using the string class names so as not to include Finagle dependencies.
    case e: Throwable if HttpClientException.isRetryableFinagleException(e) =>
      retryableFailure(e)
  }

  /**
    * Root cause exception classifier for JVM platform. This handles java.lang.reflect exceptions.
    */
  override def rootCauseExceptionClassifier: PartialFunction[Throwable, Failed] = {
    case e: ExecutionException if e.getCause != null =>
      HttpClientException.classifyExecutionFailure(e.getCause)
    case e: InvocationTargetException =>
      HttpClientException.classifyExecutionFailure(e.getTargetException)
    case e if e.getCause != null =>
      // Trace the true cause
      HttpClientException.classifyExecutionFailure(e.getCause)
  }
}
