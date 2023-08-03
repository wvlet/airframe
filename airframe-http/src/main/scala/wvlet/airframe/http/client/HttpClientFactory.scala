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

import wvlet.airframe.control.CircuitBreaker
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.{RPCEncoding, RxHttpFilter}

import scala.concurrent.duration.Duration

/**
  * Interface for customizing config for each requests
  *
  * @tparam ClientImpl
  */
trait HttpClientFactory[ClientImpl] {

  protected def config: HttpClientConfig

  /**
    * Create a new client sharing the same underlying http client
    * @param newConfig
    * @return
    */
  protected def build(newConfig: HttpClientConfig): ClientImpl

  def withRequestFilter(requestFilter: Request => Request): ClientImpl = {
    build(config.withRequestFilter(requestFilter))
  }
  def withResponseFilter(responseFilter: Response => Response): ClientImpl = {
    build(config.withResponseFilter(responseFilter))
  }
  def withClientFilter(filter: HttpClientFilter): ClientImpl = {
    build(config.withClientFilter(filter))
  }
  def withClientFilter(filter: RxHttpFilter): ClientImpl = {
    build(config.withClientFilter(filter))
  }
  def withRetryContext(filter: RetryContext => RetryContext): ClientImpl = {
    build(config.withRetryContext(filter))
  }
  def withRPCEncoding(rpcEncoding: RPCEncoding): ClientImpl = {
    build(config.withRPCEncoding(rpcEncoding))
  }
  def withConfig(filter: HttpClientConfig => HttpClientConfig): ClientImpl = {
    build(filter(config))
  }
  def withConnectTimeout(duration: Duration): ClientImpl = {
    build(config.withConnectTimeout(duration))
  }
  def withReadTimeout(duration: Duration): ClientImpl = {
    build(config.withReadTimeout(duration))
  }
  def withCircuitBreaker(filter: CircuitBreaker => CircuitBreaker): ClientImpl = {
    build(config.withCircuitBreaker(filter))
  }
}
