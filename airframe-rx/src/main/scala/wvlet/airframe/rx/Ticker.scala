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
package wvlet.airframe.rx

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
  * Ticker is for measuring the elapsed time.
  */
trait Ticker {
  // Return the number of nanoseconds elapsed
  def read: Long
}

/**
  * A Ticker implementation that can be incremented manually for testing purpose
  *
  *  This implementation is similar to FakeTicker in Guava: https://github.com/google/guava/blob/master/guava-testlib/src/com/google/common/testing/FakeTicker.java
  */
case class ManualTicker(nanos: AtomicLong = new AtomicLong(0), autoIncrementStepNanos: Long = 0) extends Ticker {

  /**
    * Set the auto-increment step, which will be added after reading a value
    */
  def withIncrements(time: Long, unit: TimeUnit): ManualTicker = {
    this.copy(autoIncrementStepNanos = unit.toNanos(time))
  }

  /**
    * Advance the ticker for the given amount
    * @param time
    * @param unit
    * @return
    */
  def advance(time: Long, unit: TimeUnit): ManualTicker = {
    nanos.addAndGet(unit.toNanos(time))
    this
  }

  /**
    *  Advance the ticker for the given nanoseconds
    * @param nanoseconds
    * @return
    */
  def advance(nanoseconds: Long): ManualTicker = {
    advance(nanoseconds, TimeUnit.NANOSECONDS)
  }
  def read: Long = {
    nanos.getAndAdd(autoIncrementStepNanos)
  }
}

object Ticker {
  // A ticker that reads the current time using System.nanoTime()
  def systemTicker: Ticker = {
    new Ticker {
      override def read: Long = System.nanoTime()
    }
  }

  /**
    * Create a testing ticker that can be manually advanced
    */
  def manualTicker: ManualTicker = {
    ManualTicker()
  }
}
