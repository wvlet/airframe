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
import scala.concurrent.duration.Duration

/**
 * Rate limiter exception thrown when rate limit is exceeded and blocking is not allowed
 */
case class RateLimitExceededException(message: String) extends Exception(message)

/**
 * A rate limiter that controls the rate of permit acquisition using a token bucket algorithm.
 * This implementation is thread-safe and works across JVM, JS, and Native platforms.
 */
class RateLimiter(
    permitsPerSecond: Double,
    maxBurstSize: Long = -1, // -1 means same as permitsPerSecond
    ticker: Ticker = Ticker.systemTicker
) {
  require(permitsPerSecond > 0, s"permitsPerSecond must be positive: ${permitsPerSecond}")

  private val actualMaxBurstSize = if (maxBurstSize == -1) permitsPerSecond.toLong.max(1) else maxBurstSize
  private val intervalNanos = (1e9 / permitsPerSecond).toLong
  
  // Token bucket state
  private val availableTokens = new AtomicReference[Double](actualMaxBurstSize.toDouble)
  private val lastRefillTime = new AtomicLong(ticker.read)

  /**
   * Acquire a single permit, blocking if necessary until permit is available.
   * @return the time spent waiting in nanoseconds
   */
  def acquire(): Long = acquire(1)

  /**
   * Acquire the specified number of permits, blocking if necessary.
   * @param permits number of permits to acquire
   * @return the time spent waiting in nanoseconds  
   */
  def acquire(permits: Int): Long = {
    require(permits > 0, s"permits must be positive: ${permits}")
    
    val waitTimeNanos = reserveAndGetWaitTime(permits, 0)
    if (waitTimeNanos > 0) {
      sleepNanos(waitTimeNanos)
    }
    waitTimeNanos
  }

  /**
   * Try to acquire a single permit without blocking.
   * @return true if permit was acquired, false otherwise
   */
  def tryAcquire(): Boolean = tryAcquire(1)

  /**
   * Try to acquire the specified number of permits without blocking.
   * @param permits number of permits to acquire
   * @return true if permits were acquired, false otherwise
   */
  def tryAcquire(permits: Int): Boolean = {
    require(permits > 0, s"permits must be positive: ${permits}")
    
    val waitTimeNanos = reserveAndGetWaitTime(permits, 0)
    waitTimeNanos == 0
  }

  /**
   * Try to acquire permits with a timeout.
   * @param permits number of permits to acquire
   * @param timeout maximum time to wait
   * @param unit time unit for the timeout
   * @return true if permits were acquired within the timeout, false otherwise
   */
  def tryAcquire(permits: Int, timeout: Long, unit: TimeUnit): Boolean = {
    require(permits > 0, s"permits must be positive: ${permits}")
    require(timeout >= 0, s"timeout must be non-negative: ${timeout}")
    
    val timeoutNanos = unit.toNanos(timeout)
    val waitTimeNanos = reserveAndGetWaitTime(permits, 0)
    
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

  private def reserveAndGetWaitTime(permits: Int, retries: Int = 0): Long = {
    if (retries > 10) {
      // Prevent infinite recursion - just try once more after yielding
      Thread.`yield`()
      val now = ticker.read
      val currentTokens = refillAndGetTokens(now)
      if (currentTokens >= permits) {
        if (availableTokens.compareAndSet(currentTokens, currentTokens - permits)) {
          0L
        } else {
          ((permits - currentTokens) * intervalNanos).toLong // Fallback calculation
        }
      } else {
        val tokensNeeded = permits - currentTokens
        availableTokens.compareAndSet(currentTokens, currentTokens - permits)
        (tokensNeeded * intervalNanos).toLong
      }
    } else {
      val now = ticker.read
      val currentTokens = refillAndGetTokens(now)
      
      println(s"[DEBUG] Current tokens: ${currentTokens}, permits needed: ${permits}")
      
      if (currentTokens >= permits) {
        // Try to consume tokens atomically
        if (availableTokens.compareAndSet(currentTokens, currentTokens - permits)) {
          println(s"[DEBUG] Successfully consumed ${permits} tokens, wait time: 0")
          0L // No wait needed
        } else {
          println(s"[DEBUG] CAS failed, retrying...")
          // Retry due to concurrent modification
          reserveAndGetWaitTime(permits, retries + 1)
        }
      } else {
        // Not enough tokens, calculate wait time
        val tokensNeeded = permits - currentTokens
        val waitTimeNanos = (tokensNeeded * intervalNanos).toLong
        println(s"[DEBUG] Not enough tokens, need ${tokensNeeded} more, wait time: ${waitTimeNanos}")
        
        // Reserve future tokens by going negative
        if (availableTokens.compareAndSet(currentTokens, currentTokens - permits)) {
          waitTimeNanos
        } else {
          println(s"[DEBUG] CAS failed in negative path, retrying...")
          // Retry due to concurrent modification  
          reserveAndGetWaitTime(permits, retries + 1)
        }
      }
    }
  }
  
  private def refillAndGetTokens(now: Long): Double = {
    val lastRefill = lastRefillTime.get()
    val timeDeltaNanos = now - lastRefill
    
    if (timeDeltaNanos <= 0) {
      return availableTokens.get()
    }
    
    val tokensToAdd = timeDeltaNanos.toDouble / intervalNanos
    if (tokensToAdd < 1e-6) {
      return availableTokens.get()
    }
    
    val currentTokens = availableTokens.get()
    val newTokens = math.min(actualMaxBurstSize.toDouble, currentTokens + tokensToAdd)
    
    // Try to update both tokens and time
    if (availableTokens.compareAndSet(currentTokens, newTokens)) {
      lastRefillTime.compareAndSet(lastRefill, now)
      newTokens
    } else {
      // If CAS failed, just return the current value
      availableTokens.get()
    }
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
   * @param permitsPerSecond the rate at which permits are replenished
   * @return a new RateLimiter instance
   */
  def create(permitsPerSecond: Double): RateLimiter = {
    new RateLimiter(permitsPerSecond)
  }

  /**
   * Create a rate limiter with the specified permits per second and max burst size.
   * @param permitsPerSecond the rate at which permits are replenished  
   * @param maxBurstSize maximum number of permits that can be accumulated
   * @return a new RateLimiter instance
   */
  def create(permitsPerSecond: Double, maxBurstSize: Long): RateLimiter = {
    new RateLimiter(permitsPerSecond, maxBurstSize)
  }
}