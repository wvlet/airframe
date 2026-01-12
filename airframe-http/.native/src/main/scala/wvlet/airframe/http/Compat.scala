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

import wvlet.airframe.control.ResultClass.Failed
import wvlet.airframe.http.client.{HttpClientBackend, NativeHttpClientBackend}
import wvlet.airframe.http.internal.LocalRPCContext

import scala.concurrent.ExecutionContext

/**
  * Scala Native specific implementation of HTTP compatibility layer.
  *
  * This implementation uses libcurl for HTTP client operations and provides platform-specific utilities for URL
  * encoding, execution contexts, and RPC context management.
  */
private object Compat extends CompatApi {

  /**
    * URL encode a string using percent-encoding.
    *
    * This implementation manually encodes characters following RFC 3986, since Scala Native doesn't have direct access
    * to java.net.URLEncoder.
    */
  override def urlEncode(s: String): String = {
    if (s == null || s.isEmpty) {
      s
    } else {
      val sb = new StringBuilder()
      val bytes = s.getBytes("UTF-8")
      for (b <- bytes) {
        val c = b.toChar
        if (isUnreserved(c)) {
          sb.append(c)
        } else {
          // Percent-encode the byte
          sb.append('%')
          sb.append(toHexChar((b >> 4) & 0x0f))
          sb.append(toHexChar(b & 0x0f))
        }
      }
      sb.toString()
    }
  }

  /**
    * Check if a character is unreserved per RFC 3986. Unreserved characters don't need encoding.
    */
  private def isUnreserved(c: Char): Boolean = {
    (c >= 'A' && c <= 'Z') ||
    (c >= 'a' && c <= 'z') ||
    (c >= '0' && c <= '9') ||
    c == '-' || c == '_' || c == '.' || c == '~'
  }

  private def toHexChar(i: Int): Char = {
    if (i < 10) ('0' + i).toChar
    else ('A' + (i - 10)).toChar
  }

  /**
    * Scala Native doesn't have a notion of host server address (unlike browsers in Scala.js).
    */
  override def hostServerAddress: ServerAddress = ServerAddress.empty

  /**
    * Return the libcurl-based HTTP client backend for Scala Native.
    */
  override def defaultHttpClientBackend: HttpClientBackend = NativeHttpClientBackend

  /**
    * Return the default execution context for Scala Native.
    *
    * Uses scala.concurrent.ExecutionContext.global which is available in Scala Native.
    */
  override def defaultExecutionContext: ExecutionContext = {
    ExecutionContext.global
  }

  /**
    * Return a factory for creating HTTP loggers.
    *
    * For Scala Native, we use ConsoleHttpLogger which outputs to debug logs.
    */
  override def defaultHttpClientLoggerFactory: HttpLoggerConfig => HttpLogger = { (config: HttpLoggerConfig) =>
    new HttpLogger.ConsoleHttpLogger(config)
  }

  /**
    * Get the current thread-local RPC context.
    */
  override def currentRPCContext: RPCContext = LocalRPCContext.current

  /**
    * Attach a new RPC context and return the previous context.
    */
  override def attachRPCContext(context: RPCContext): RPCContext = LocalRPCContext.attach(context)

  /**
    * Detach the current RPC context and restore the previous one.
    */
  override def detachRPCContext(previous: RPCContext): Unit = LocalRPCContext.detach(previous)

  /**
    * SSL exception classifier for Scala Native. Returns an empty classifier since javax.net.ssl classes are not
    * available on Native.
    */
  override def sslExceptionClassifier: PartialFunction[Throwable, Failed] = PartialFunction.empty

  /**
    * Connection exception classifier for Scala Native. Returns an empty classifier since java.net exception classes
    * may not be fully available on Native.
    */
  override def connectionExceptionClassifier: PartialFunction[Throwable, Failed] = PartialFunction.empty

  /**
    * Root cause exception classifier for Scala Native. Uses a simpler implementation without java.lang.reflect.
    */
  override def rootCauseExceptionClassifier: PartialFunction[Throwable, Failed] = {
    case e if e.getCause != null =>
      // Trace the true cause
      HttpClientException.classifyExecutionFailure(e.getCause)
  }
}
