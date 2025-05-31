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

import java.util.concurrent.atomic.{AtomicLong, AtomicReference}
import java.util.concurrent.TimeUnit

/**
  * Rate limiter exception thrown when rate limit is exceeded and blocking is not allowed
  */
case class RateLimitExceededException(message: String) extends Exception(message)

/**
  * Internal state for the rate limiter
  */
private case class RateLimiterState(
    availableTokens: Double,
    lastRefillTimeNanos: Long
)

/**
  * A rate limiter that controls the rate of permit acquisition using a token bucket algorithm. This implementation is
  * thread-safe and works across JVM, JS, and Native platforms.
  */
class RateLimiter(
    permitsPerSecond: Double,
    maxBurstSize: Long = -1, // -1 means same as permitsPerSecond
    ticker: Ticker = Ticker.systemTicker
) {
  require(permitsPerSecond > 0, s"permitsPerSecond must be positive: ${permitsPerSecond}")

  private val actualMaxBurstSize = if (maxBurstSize == -1) permitsPerSecond.toLong.max(1) else maxBurstSize
  private val intervalNanos      = (1e9 / permitsPerSecond).toLong

  // Using a single atomic reference to hold both tokens and timestamp
  private val state = new AtomicReference[RateLimiterState](
    RateLimiterState(actualMaxBurstSize.toDouble, ticker.read)
  )

  /**
    * Acquire a single permit, blocking if necessary until permit is available.
    * @return
    *   the time spent waiting in nanoseconds
    */
  def acquire(): Long = acquire(1)

  /**
    * Acquire the specified number of permits, blocking if necessary.
    * @param permits
    *   number of permits to acquire
    * @return
    *   the time spent waiting in nanoseconds
    */
  def acquire(permits: Int): Long = {
    require(permits > 0, s"permits must be positive: ${permits}")

    val waitTimeNanos = reservePermits(permits)
    if (waitTimeNanos > 0) {
      sleepNanos(waitTimeNanos)
    }
    waitTimeNanos
  }

  /**
    * Try to acquire a single permit without blocking.
    * @return
    *   true if permit was acquired, false otherwise
    */
  def tryAcquire(): Boolean = tryAcquire(1)

  /**
    * Try to acquire the specified number of permits without blocking.
    * @param permits
    *   number of permits to acquire
    * @return
    *   true if permits were acquired, false otherwise
    */
  def tryAcquire(permits: Int): Boolean = {
    require(permits > 0, s"permits must be positive: ${permits}")

    val waitTimeNanos = tryReservePermits(permits)
    waitTimeNanos == 0
  }

  /**
    * Try to acquire permits with a timeout.
    * @param permits
    *   number of permits to acquire
    * @param timeout
    *   maximum time to wait
    * @param unit
    *   time unit for the timeout
    * @return
    *   true if permits were acquired within the timeout, false otherwise
    */
  def tryAcquire(permits: Int, timeout: Long, unit: TimeUnit): Boolean = {
    require(permits > 0, s"permits must be positive: ${permits}")
    require(timeout >= 0, s"timeout must be non-negative: ${timeout}")

    val timeoutNanos  = unit.toNanos(timeout)
    val waitTimeNanos = reservePermits(permits)

    if (waitTimeNanos <= timeoutNanos) {
      if (waitTimeNanos > 0) {
        sleepNanos(waitTimeNanos)
      }
      true
    } else {
      false
    }
  }

  /**
    * Get the current rate in permits per second.
    */
  def getRate: Double = permitsPerSecond

  /**
    * Create a new RateLimiter with a different rate.
    */
  def withRate(newPermitsPerSecond: Double): RateLimiter = {
    new RateLimiter(newPermitsPerSecond, maxBurstSize, ticker)
  }

  /**
    * Create a new RateLimiter with a different max burst size.
    */
  def withMaxBurstSize(newMaxBurstSize: Long): RateLimiter = {
    new RateLimiter(permitsPerSecond, newMaxBurstSize, ticker)
  }

  /**
    * Try to reserve permits without allowing the bucket to go negative. This is used for non-blocking operations like
    * tryAcquire.
    */
  private def tryReservePermits(permits: Int): Long = {
    var retry = 0
    while (retry < 100) { // Limit retries to prevent infinite loops
      val currentState = state.get()
      val now          = ticker.read

      // Calculate new state after refilling tokens
      val newState = refillTokens(currentState, now)

      if (newState.availableTokens >= permits) {
        // Enough tokens available, try to consume them
        val updatedState = newState.copy(availableTokens = newState.availableTokens - permits)
        if (state.compareAndSet(currentState, updatedState)) {
          return 0L // Success, no wait needed
        }
      } else {
        // Not enough tokens available - for tryAcquire, we don't allow going negative
        return 1L // Any positive value indicates failure for tryAcquire
      }

      retry += 1
    }

    // Fallback: if we couldn't complete the operation after many retries
    1L // Indicate failure
  }

  /**
    * Reserve permits and return the wait time needed. This is the core method that handles token bucket logic.
    */
  private def reservePermits(permits: Int): Long = {
    var retry = 0
    while (retry < 100) { // Limit retries to prevent infinite loops
      val currentState = state.get()
      val now          = ticker.read

      // Calculate new state after refilling tokens
      val newState = refillTokens(currentState, now)

      if (newState.availableTokens >= permits) {
        // Enough tokens available, try to consume them
        val updatedState = newState.copy(availableTokens = newState.availableTokens - permits)
        if (state.compareAndSet(currentState, updatedState)) {
          return 0L // Success, no wait needed
        }
      } else {
        // Not enough tokens, calculate wait time and reserve future tokens
        val tokensNeeded  = permits - newState.availableTokens
        val waitTimeNanos = (tokensNeeded * intervalNanos).toLong
        val updatedState  = newState.copy(availableTokens = newState.availableTokens - permits)

        if (state.compareAndSet(currentState, updatedState)) {
          return waitTimeNanos
        }
      }

      retry += 1
    }

    // Fallback: if we couldn't complete the operation after many retries,
    // just return a reasonable wait time
    (permits * intervalNanos).toLong
  }

  /**
    * Refill tokens based on elapsed time and return the new state.
    */
  private def refillTokens(oldState: RateLimiterState, currentTimeNanos: Long): RateLimiterState = {
    val timeDeltaNanos = currentTimeNanos - oldState.lastRefillTimeNanos

    if (timeDeltaNanos <= 0) {
      return oldState
    }

    val tokensToAdd = timeDeltaNanos.toDouble / intervalNanos
    if (tokensToAdd < 1e-6) {
      return oldState // Avoid tiny refills
    }

    val newTokens = math.min(actualMaxBurstSize.toDouble, oldState.availableTokens + tokensToAdd)
    RateLimiterState(newTokens, currentTimeNanos)
  }

  private def sleepNanos(nanos: Long): Unit = {
    if (nanos <= 0) return

    val millis = nanos / 1000000

    try {
      if (millis > 0) {
        Compat.sleep(millis)
      }
      // For sub-millisecond precision, we skip the nanosecond part since
      // cross-platform support is limited. This is acceptable for rate limiting.
    } catch {
      case _: InterruptedException =>
        Thread.currentThread().interrupt()
    }
  }
}

/**
  * RateLimiter companion object with factory methods
  */
object RateLimiter {

  /**
    * Create a rate limiter with the specified permits per second.
    * @param permitsPerSecond
    *   the rate at which permits are replenished
    * @return
    *   a new RateLimiter instance
    */
  def create(permitsPerSecond: Double): RateLimiter = {
    new RateLimiter(permitsPerSecond)
  }

  /**
    * Create a rate limiter with the specified permits per second and max burst size.
    * @param permitsPerSecond
    *   the rate at which permits are replenished
    * @param maxBurstSize
    *   maximum number of permits that can be accumulated
    * @return
    *   a new RateLimiter instance
    */
  def create(permitsPerSecond: Double, maxBurstSize: Long): RateLimiter = {
    new RateLimiter(permitsPerSecond, maxBurstSize)
  }
}
