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

import wvlet.airframe.AirframeSpec

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.util.Try

class ParallelTest extends AirframeSpec {

  "Parallel" should {
    "run() in parallel with Seq" in {
      val source = Seq(1, 2, 3)
      val start  = System.currentTimeMillis()
      val result = Parallel.run(source, parallelism = 3) { i =>
        Thread.sleep(500)
        i * 2
      }
      val duration = System.currentTimeMillis() - start
      assert(duration < 2000)
      assert(result == List(2, 4, 6))
    }

    "iterate() in parallel with Iterator" in {
      val source = Seq(1, 2, 3)
      val start  = System.currentTimeMillis()
      val result = Parallel.iterate(source.toIterator, parallelism = 3) { i =>
        Thread.sleep(500 * i)
        i * 2
      }

      // wait for completion here
      val list = result.toList

      val duration = System.currentTimeMillis() - start
      assert(duration < 2000)
      assert(list == List(2, 4, 6))
    }

    "handle errors in run()" in {
      val source    = Seq(1, 2, 3)
      val exception = new RuntimeException("failure")

      val result = Parallel.run(source, parallelism = 3) { i =>
        Try {
          if (i == 2) {
            throw exception
          }
          i * 2
        }
      }

      assert(result == List(Success(2), Failure(exception), Success(6)))
    }

    "handle errors in iterate()" in {
      val source    = Seq(1, 2, 3)
      val exception = new RuntimeException("failure")

      val result = Parallel.iterate(source.toIterator, parallelism = 3) { i =>
        Try {
          if (i == 2) {
            throw exception
          }
          i * 2
        }
      }

      // wait for completion here
      val list = result.toList

      assert(list == List(Success(2), Failure(exception), Success(6)))
    }

    "repeat() and stop()" in {
      val source  = Seq(0, 2, 5)
      val counter = scala.collection.mutable.HashMap[Int, Int]()
      val stoppable = Parallel.repeat(source, interval = 1 second) { e =>
        counter.update(e, counter.get(e).getOrElse(0) + 1)
        Thread.sleep(e * 1000)
      }

      Thread.sleep(4900)

      stoppable.stop()

      Thread.sleep(1000)

      assert(counter(0) == 5)
      assert(counter(2) == 3)
      assert(counter(5) == 1)
    }
  }
}
