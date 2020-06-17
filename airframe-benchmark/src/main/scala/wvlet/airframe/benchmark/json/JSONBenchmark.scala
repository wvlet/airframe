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
package wvlet.airframe.benchmark.json

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import wvlet.airframe.json.JSON.{JSONArray, JSONString}
import wvlet.airframe.json.{JSON, JSONSource, JSONTraverser, JSONVisitor, NullJSONContext}
import wvlet.log.io.{IOUtil, Timer}

import scala.util.Random

abstract class JSONParseBenchmark {
  protected val json: String

  @Benchmark
  def airframe(blackhole: Blackhole): Unit = {
    blackhole.consume(JSONBenchmark.airframeParse(json))
  }

  @Benchmark
  def circe(blackhole: Blackhole): Unit = {
    blackhole.consume(JSONBenchmark.circeParse(json))
  }

  @Benchmark
  def json4sJackson(blackhole: Blackhole): Unit = {
    blackhole.consume(JSONBenchmark.json4sJacksonParse(json))
  }
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class TwitterJSON extends JSONParseBenchmark {
  override protected val json: String = JSONBenchmark.twitterJson
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class BooleanArray extends JSONParseBenchmark {
  override protected val json: String = JSONBenchmark.jsonBooleanArray
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class StringArray extends JSONParseBenchmark {
  override protected val json: String = JSONBenchmark.jsonStringArray
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class LongArray extends JSONParseBenchmark {
  override protected val json: String = JSONBenchmark.jsonIntArray
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class DoubleArray extends JSONParseBenchmark {
  override protected val json: String = JSONBenchmark.jsonDoubleArray
}

/**
  */
object JSONBenchmark extends Timer {
  val r1 = new Random(0)
  val r2 = new Random(0)
  val r3 = new Random(0)

  def twitterJson       = IOUtil.readAsString("twitter.json")
  def twitterSingleJson = IOUtil.readAsString("twitter-single.json")
  def jsonBooleanArray  = s"[${(0 until 100).map(_ => r1.nextBoolean()).mkString(",")}]"
  def jsonIntArray      = s"[${(0 until 100).map(_ => r2.nextLong()).mkString(",")}]"
  def jsonDoubleArray   = s"[${(0 until 100).map(_ => r3.nextDouble()).mkString(",")}]"
  def jsonStringArray = {
    // Extract JSON strings from twitter.json
    val j = JSON.parse(twitterJson)
    val b = IndexedSeq.newBuilder[JSONString]
    JSONTraverser.traverse(
      j,
      new JSONVisitor {
        override def visitKeyValue(k: String, v: JSON.JSONValue): Unit = {
          b += JSONString(k)
        }
        override def visitString(v: JSON.JSONString): Unit = {
          b += v
        }
      }
    )
    val jsonArray = JSONArray(b.result()).toJSON
    jsonArray
  }

  def runAll(N: Int = 10, B: Int = 10): Unit = {
    bench("twitter.json", twitterJson, N = N, B = N)
    bench("boolean array", jsonBooleanArray, N = N, B = B)
    bench("int array", jsonIntArray, N = N, B = B)
    bench("double array", jsonDoubleArray, N = N, B = B)
    bench("string array", jsonStringArray, N = N, B = B)
  }

  def airframeScanOnly(json: String): Unit = {
    val jsonSource = JSONSource.fromString(json)
    wvlet.airframe.json.JSONScanner.scan(jsonSource, new NullJSONContext(isObject = true))
  }

  def airframeParse(json: String): Unit = {
    wvlet.airframe.json.JSON.parse(json)
  }

  def circeParse(json: String): Unit = {
    io.circe.parser.parse(json)
  }

  def json4sJacksonParse(json: String): Unit = {
    org.json4s.jackson.JsonMethods.parse(json)
  }

  private def bench(benchName: String, json: String, N: Int, B: Int): Unit = {
    time(benchName, repeat = N, blockRepeat = B) {
      block("airframe scan-only") {
        airframeScanOnly(json)
      }
      block("airframe          ") {
        airframeParse(json)
      }
      block("circe             ") {
        circeParse(json)
      }
      block("json4s-jackson    ") {
        json4sJacksonParse(json)
      }
    }
  }
}
