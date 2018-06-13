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
package wvlet.airframe.control

import Retry._

/**
  *
  */
object Control {

  def withBackOffRetry[A](maxRetry: Int = 10,
                          initialIntervalMillis: Int = 100,
                          maxIntervalMillis: Int = 15000,
                          multiplier: Double = 1.5)(body: => A): Code[A] = {
    val config = RetryConfig(initialIntervalMillis, maxIntervalMillis, multiplier)
    withRetry(maxRetry, new ExponentialBackOff(config))(body)
  }

  def withJitterRetry[A](maxRetry: Int = 10,
                         initialIntervalMillis: Int = 100,
                         maxIntervalMillis: Int = 15000,
                         multiplier: Double = 1.5)(body: => A): Code[A] = {
    val config = RetryConfig(initialIntervalMillis, maxIntervalMillis, multiplier)
    withRetry(maxRetry, new Jitter(config))(body)
  }

  private def withRetry[A](maxRetry: Int = 10, retryWaitStrategy: RetryWaitStrategy)(body: => A): Code[A] = {
    Code
      .deffered(body)
      .rescueWith { e: Throwable =>
        if (maxRetry > 0) {
          Code
            .sleepMillis(retryWaitStrategy.nextWaitMillis)
            .andThen {
              withRetry(maxRetry - 1, retryWaitStrategy)(body)
            }
        } else {
          Code.raiseError(e)
        }
      }
  }
}
