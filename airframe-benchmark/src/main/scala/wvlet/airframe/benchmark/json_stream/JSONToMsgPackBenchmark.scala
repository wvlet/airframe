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
package wvlet.airframe.benchmark.json_stream

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import wvlet.airframe.benchmark.json.JSONBenchmark
import wvlet.airframe.codec.JSONValueCodec
import wvlet.airframe.json.{JSON, JSONSource}
import wvlet.airframe.msgpack.json.{NestedMessagePackBuilder, StreamMessagePackBuilder}
import wvlet.airframe.msgpack.spi.MessagePack

/**
  */
abstract class JSONToMsgPackBenchmarkBase {
  protected val json: String

  @Benchmark
  def jsonValue(blackhole: Blackhole): Unit = {
    blackhole.consume(JSONValueCodec.toMsgPack(JSON.parse(json)))
  }

  @Benchmark
  def nested(blackHole: Blackhole): Unit = {
    blackHole.consume(NestedMessagePackBuilder.fromJSON(JSONSource.fromString(json)))
  }

  @Benchmark
  def twoPass(blackHole: Blackhole): Unit = {
    blackHole.consume(StreamMessagePackBuilder.fromJSON(JSONSource.fromString(json)))
  }
}
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class TwitterJSON extends JSONToMsgPackBenchmarkBase {
  override protected val json: String = JSONBenchmark.twitterJson
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class TwitterSingleJSON extends JSONToMsgPackBenchmarkBase {
  override protected val json: String = JSONBenchmark.twitterSingleJson
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class IntArraySON extends JSONToMsgPackBenchmarkBase {
  override protected val json: String = JSONBenchmark.jsonIntArray
}
