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
package wvlet.airframe.benchmark

import wvlet.airframe.AirframeSpec

/**
  *
  */
class BenchmarkMainTest extends AirframeSpec {
  private val iteration = if (inCI) 1 else 5

  "run msgpack benchmark" taggedAs ("msgpack") in {
    // Need to run without forking the JVM process as sbt cannot pass proper classpath and causes
    // "Could not find or load main class org.openjdk.jmh.runner.ForkedMain" error
    BenchmarkMain.main(s"bench-quick -i ${iteration} -F 0 msgpack")
  }

  "run json benchmark" taggedAs ("json") in {
    // Need to run without forking the JVM process as sbt cannot pass proper classpath and causes
    // "Could not find or load main class org.openjdk.jmh.runner.ForkedMain" error
    BenchmarkMain.main(s"bench-quick -i ${iteration} -F 0 json")
  }

  "run JSON elapsed-time benchmark" taggedAs ("json-time") in {
    BenchmarkMain.main(s"json-perf -n ${iteration} -b ${iteration}")
  }
}
