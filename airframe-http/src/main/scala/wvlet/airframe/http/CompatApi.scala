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

import wvlet.airframe.http.client.HttpClientBackend
import wvlet.airframe.control.ResultClass.Failed

import scala.concurrent.ExecutionContext

/**
  * An interface for using different implementation between Scala JVM and Scala.js
  */
private[http] trait CompatApi {
  def urlEncode(s: String): String

  def hostServerAddress: ServerAddress = ServerAddress.empty
  def defaultHttpClientBackend: HttpClientBackend
  def defaultExecutionContext: ExecutionContext
  def defaultHttpClientLoggerFactory: HttpLoggerConfig => HttpLogger

  def currentRPCContext: RPCContext
  def attachRPCContext(context: RPCContext): RPCContext
  def detachRPCContext(previous: RPCContext): Unit

  /**
    * Platform-specific SSL exception classifier for retry logic. JVM provides full SSL exception handling, while
    * Native/JS return an empty classifier since javax.net.ssl classes are not available.
    */
  def sslExceptionClassifier: PartialFunction[Throwable, Failed]

  /**
    * Platform-specific connection exception classifier for retry logic. JVM provides full java.net exception handling,
    * while Native/JS return an empty classifier since these classes may not be fully available.
    */
  def connectionExceptionClassifier: PartialFunction[Throwable, Failed]

  /**
    * Platform-specific root cause exception classifier for retry logic. JVM provides java.lang.reflect handling, while
    * Native/JS return a simpler classifier.
    */
  def rootCauseExceptionClassifier: PartialFunction[Throwable, Failed]
}
