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

import com.chatwork.scala.ulid.{ULID => ChatworkULID}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import wvlet.airframe.ulid.{ULID => AirframeULID}

import java.util.concurrent.TimeUnit

abstract class ULIDBenchmark {
  protected def newMonotonicULIDString: String

  @Benchmark
  @Group("generateMonotonic")
  def generateMonotonic(blackhole: Blackhole): Unit = {
    blackhole.consume(newMonotonicULIDString)
  }
}

@Threads(4)
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class Airframe extends ULIDBenchmark {
  override protected def newMonotonicULIDString: String = {
    AirframeULID.newULID.toString
  }
}

@Threads(4)
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class AirframeNonSecure extends ULIDBenchmark {
  private val gen = AirframeULID.nonSecureRandomULIDGenerator

  override protected def newMonotonicULIDString: String = {
    gen.newULID.toString
  }
}

@Threads(4)
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class Chatwork extends ULIDBenchmark {
  private var lastValue: ChatworkULID = ChatworkULID.generate()

  override protected def newMonotonicULIDString: String = {
    var newValue: Option[ChatworkULID] = None
    synchronized {
      newValue = ChatworkULID.generateStrictlyMonotonic(lastValue)
      while (newValue.isEmpty) {
        newValue = ChatworkULID.generateStrictlyMonotonic(lastValue)
      }
      lastValue = newValue.get
    }
    newValue.get.asString
  }

  @Benchmark
  @Group("generate")
  protected def generateNewULIDString(blackhole: Blackhole): Unit = {
    blackhole.consume(ChatworkULID.generate().toString)
  }
}

@Threads(4)
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class UUID {
  @Benchmark
  @Group("generate")
  def generate(blackhole: Blackhole): Unit = {
    blackhole.consume(java.util.UUID.randomUUID().toString)
  }
}
