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
package wvlet.airframe.benchmark.msgpack

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class MsgpackBenchmark {
  /*
   * Suppose we want to measure how much it takes to sum two integers:
   */

  val x = 1
  val y = 2

  /*
   * This is what you do with JMH.
   */

  @Benchmark
  def measureRight: Int = x + y

  /*
   * The following tests emulate the naive looping.
   * This is the Caliper-style benchmark.
   */
  private def reps(reps: Int): Int = {
    var s = 0
    var i = 0
    while (i < reps) {
      s += (x + y)
      i += 1
    }
    s
  }

  /*
   * We would like to measure this with different repetitions count.
   * Special annotation is used to get the individual operation cost.
   */

  @Benchmark
  @OperationsPerInvocation(1)
  def measureWrong_1: Int = reps(1)
}
