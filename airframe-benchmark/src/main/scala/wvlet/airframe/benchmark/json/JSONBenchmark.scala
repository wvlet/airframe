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
  @Group("json_parse")
  def airframe(blackhole: Blackhole): Unit = {
    blackhole.consume(JSONBenchmark.airframeParse(json))
  }

  @Benchmark
  @Group("json_parse")
  def circe(blackhole: Blackhole): Unit = {
    blackhole.consume(JSONBenchmark.circeParse(json))
  }

  @Benchmark
  @Group("json_parse")
  def jawn(blackhole: Blackhole): Unit = {
    blackhole.consume(JSONBenchmark.jawnParse(json))
  }

  @Benchmark
  @Group("json_parse")
  def json4sJackson(blackhole: Blackhole): Unit = {
    blackhole.consume(JSONBenchmark.json4sJacksonParse(json))
  }

  @Benchmark
  @Group("json_parse")
  def json4sNative(blackhole: Blackhole): Unit = {
    blackhole.consume(JSONBenchmark.json4sNativeParse(json))
  }

  @Benchmark
  @Group("json_parse")
  def uJson(blackhole: Blackhole): Unit = {
    blackhole.consume(JSONBenchmark.uJsonParse(json))
  }
}

@State(Scope.Group)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class TwitterJSON extends JSONParseBenchmark {
  override protected val json: String = JSONBenchmark.twitterJson
}

@State(Scope.Group)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class BooleanArray extends JSONParseBenchmark {
  override protected val json: String = JSONBenchmark.jsonBooleanArray
}

@State(Scope.Group)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class StringArray extends JSONParseBenchmark {
  override protected val json: String = JSONBenchmark.jsonStringArray
}

@State(Scope.Group)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class LongArray extends JSONParseBenchmark {
  override protected val json: String = JSONBenchmark.jsonIntArray
}

@State(Scope.Group)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class DoubleArray extends JSONParseBenchmark {
  override protected val json: String = JSONBenchmark.jsonDoubleArray
}

/**
  *
  */
object JSONBenchmark extends Timer {

  def twitterJson      = IOUtil.readAsString("twitter.json")
  def jsonBooleanArray = s"[${(0 until 10000).map(_ => Random.nextBoolean()).mkString(",")}]"
  def jsonIntArray     = s"[${(0 until 10000).map(_ => Random.nextLong()).mkString(",")}]"
  def jsonDoubleArray  = s"[${(0 until 10000).map(_ => Random.nextDouble()).mkString(",")}]"
  def jsonStringArray = {
    // Extract JSON strings from twitter.json
    val j = JSON.parse(twitterJson)
    val b = IndexedSeq.newBuilder[JSONString]
    JSONTraverser.traverse(j, new JSONVisitor {
      override def visitKeyValue(k: String, v: JSON.JSONValue): Unit = {
        b += JSONString(k)
      }
      override def visitString(v: JSON.JSONString): Unit = {
        b += v
      }
    })
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

  def jawnParse(json: String): Unit = {
    new io.circe.jawn.JawnParser().parse(json)
  }

  def json4sJacksonParse(json: String): Unit = {
    org.json4s.jackson.JsonMethods.parse(json)
  }

  def json4sNativeParse(json: String): Unit = {
    org.json4s.native.JsonMethods.parse(json)
  }

  def uJsonParse(json: String): Unit = {
    ujson.read(json)
  }

  private def bench(benchName: String, json: String, N: Int, B: Int): Unit = {
    val jsonSource = JSONSource.fromString(json)
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
      block("jawn              ") {
        jawnParse(json)
      }
      block("json4s-jackson    ") {
        json4sJacksonParse(json)
      }
      block("json4s-native     ") {
        json4sNativeParse(json)
      }
      block("uJson             ") {
        uJsonParse(json)
      }
    }
  }

}
