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
package wvlet.airframe.examples.control

import scala.concurrent.TimeoutException

/**
  * Adding randomness to the retry interval of exponential backoff is good to spread out
  * retry interval timings. See also https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
  */
object Control_02_Jitter {

  import wvlet.airframe.control.Retry

  Retry
    .withJitter(maxRetry = 3) // It will wait nextWaitMillis * rand() upon retry
    .retryOn {
      case e: TimeoutException =>
        Retry.retryableFailure(e)
    }
    .run {
      // body
    }

}
