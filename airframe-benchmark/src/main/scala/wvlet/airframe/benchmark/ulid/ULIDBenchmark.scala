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
package wvlet.airframe.benchmark.ulid

import org.openjdk.jmh.annotations.{Benchmark, BenchmarkMode, Group, Mode, OutputTimeUnit, Scope, State}
import org.openjdk.jmh.infra.Blackhole
import wvlet.airframe.ulid.{ULID => AirframeULID}
import com.chatwork.scala.ulid.{ULID => ChatworkULID}
import java.util.concurrent.TimeUnit

abstract class ULIDBenchmark {
  protected def newMonotonicULIDString: String

  @Benchmark
  @Group("generateMonotonic")
  def generateMonotonic(blackhole: Blackhole): Unit = {
    blackhole.consume(newMonotonicULIDString)
  }
}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class Airframe extends ULIDBenchmark {
  override protected def newMonotonicULIDString: String = {
    AirframeULID.newULID.toString
  }
}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class Chatwork extends ULIDBenchmark {
  private var lastValue: ChatworkULID = ChatworkULID.generate()

  override protected def newMonotonicULIDString: String = {
    val newValue = ChatworkULID.generateMonotonic(lastValue)
    lastValue = newValue
    newValue.asString
  }

  @Benchmark
  @Group("generate")
  protected def generateNewULIDString(blackhole: Blackhole): Unit = {
    blackhole.consume(ChatworkULID.generate().toString)
  }
}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class UUID {
  @Benchmark
  @Group("generate")
  def generate(blackhole: Blackhole): Unit = {
    blackhole.consume(java.util.UUID.randomUUID().toString)
  }
}
