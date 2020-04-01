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

import java.util.concurrent.atomic.AtomicInteger

import wvlet.airspec.AirSpec

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class ParallelTest extends AirSpec {
  def `run() in parallel with Seq`: Unit = {
    Parallel.stats.startedTasks.set(0)
    Parallel.stats.finishedTasks.set(0)

    val source    = Seq(1, 2, 3)
    val counter   = new AtomicInteger(0)
    val startTime = Array(Long.MaxValue, Long.MaxValue, Long.MaxValue)
    val result = Parallel.run(source, parallelism = 3) { i =>
      // Record the current time
      startTime(i - 1) = System.currentTimeMillis()
      counter.incrementAndGet()
      while (counter.get() < 3) {
        Thread.sleep(0)
      }
      i * 2
    }
    val endTime = System.currentTimeMillis()
    assert(startTime.forall(_ <= endTime))
    assert(result == List(2, 4, 6))

    assert(Parallel.stats.startedTasks.get() == 3)
    assert(Parallel.stats.finishedTasks.get() == 3)
  }

  def `iterate() in parallel with Iterator`: Unit = {
    Parallel.stats.startedTasks.set(0)
    Parallel.stats.finishedTasks.set(0)

    val source    = Seq(1, 2, 3)
    val startTime = Array(Long.MaxValue, Long.MaxValue, Long.MaxValue)
    val result = Parallel.iterate(source.iterator, parallelism = 3) { i =>
      startTime(i - 1) = System.currentTimeMillis()
      i * 2
    }
    // wait for completion here
    val list = result.toList

    val endTime = System.currentTimeMillis()
    assert(startTime.forall(_ <= endTime))

    // The result element order can be shuffled
    assert(List(2, 4, 6).forall(x => list.contains(x)))

    assert(Parallel.stats.startedTasks.get() == 3)
    assert(Parallel.stats.finishedTasks.get() == 3)
  }

  def `handle errors in run()` : Unit = {
    Parallel.stats.startedTasks.set(0)
    Parallel.stats.finishedTasks.set(0)

    val source    = Seq(1, 2, 3)
    val exception = new RuntimeException("failure")

    val result = Parallel
      .run(source, parallelism = 3) { i =>
        Try {
          if (i == 2) {
            throw exception
          }
          i * 2
        }
      }.toList

    assert(List(Success(2), Failure(exception), Success(6)).forall(x => result.contains(x)))

    assert(Parallel.stats.startedTasks.get() == 3)
    assert(Parallel.stats.finishedTasks.get() == 3)
  }

  def `handle errors in iterate()` : Unit = {
    Parallel.stats.startedTasks.set(0)
    Parallel.stats.finishedTasks.set(0)

    val source    = Seq(1, 2, 3)
    val exception = new RuntimeException("failure")

    val result = Parallel
      .iterate(source.iterator, parallelism = 3) { i =>
        Try {
          if (i == 2) {
            throw exception
          }
          i * 2
        }
      }.toList

    assert(List(Success(2), Failure(exception), Success(6)).forall(x => result.contains(x)))

    assert(Parallel.stats.startedTasks.get() == 3)
    assert(Parallel.stats.finishedTasks.get() == 3)
  }

  def `be run for Seq using syntax sugar`: Unit = {
    import wvlet.airframe.control.parallel._

    Parallel.stats.startedTasks.set(0)
    Parallel.stats.finishedTasks.set(0)

    val source           = Seq(1, 2, 3)
    val result: Seq[Int] = source.parallel.withParallelism(2).map { x => x * 2 }

    assert(result == List(2, 4, 6))

    assert(Parallel.stats.startedTasks.get() == 3)
    assert(Parallel.stats.finishedTasks.get() == 3)
  }

  def `be run for Iterator using syntax sugar`: Unit = {
    import wvlet.airframe.control.parallel._

    Parallel.stats.startedTasks.set(0)
    Parallel.stats.finishedTasks.set(0)

    val source                = Seq(1, 2, 3).iterator
    val result: Iterator[Int] = source.parallel.withParallelism(2).map { x => x * 2 }

    val list = result.toList
    assert(List(2, 4, 6).forall(x => list.contains(x)))

    assert(Parallel.stats.startedTasks.get() == 3)
    assert(Parallel.stats.finishedTasks.get() == 3)
  }

  def `breaking execution in run()` : Unit = {
    Parallel.stats.startedTasks.set(0)
    Parallel.stats.finishedTasks.set(0)

    val result = Parallel.run(Seq(1, 2, 3), parallelism = 1) { i =>
      if (i == 2) {
        Parallel.break
      }
      i
    }

    assert(result == List(1, 0, 0))
    assert(Parallel.stats.startedTasks.get() == 2)
    assert(Parallel.stats.finishedTasks.get() == 2)
  }

  def `breaking execution in iterate()` : Unit = {
    Parallel.stats.startedTasks.set(0)
    Parallel.stats.finishedTasks.set(0)

    val result = Parallel.iterate(Seq(1, 2, 3).iterator, parallelism = 1) { i =>
      if (i == 2) {
        Parallel.break
      }
      i
    }

    // wait for completion here
    val list = result.toList

    assert(list == List(1))
    assert(Parallel.stats.startedTasks.get() == 2)
    assert(Parallel.stats.finishedTasks.get() == 2)
  }
//    def `repeat() and stop`: Unit =  {
//      val source  = Seq(0)
//
//      val ticker  = Ticker.manualTicker
//
//      var runTime: List[Long] = Nil
//      val counter  = new AtomicInteger(0)
//      val stoppable = Parallel.repeat(source, interval = 1 second, ticker = ticker) { e =>
//        runTime = ticker.read :: runTime
//        counter.incrementAndGet()
//      }
//
//      ticker.tick(0)
//      assert(runtime == 0 :: Nil)
//
//
//      ticker.tick(2 * 1000)
//      ticker.tick(5 * 1000)
//
//
//      ticker.tick(4900)
//      stoppable.stop
//      ticker.tick(1000)
//
//      assert(counter(0) == 5)
//      assert(counter(2) == 3)
//      assert(counter(5) == 1)
//    }
}
