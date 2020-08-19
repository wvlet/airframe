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

import wvlet.airspec.AirSpec

/**
  */
class BenchmarkMainTest extends AirSpec {
  if (inCI) {
    skip("Running benchmark on CI with coverage report has too much overhead")
  }

  // Never repeat test in the test.
  private val iteration = 1

  // Need to run without forking the JVM process (-F 0) as sbt cannot pass proper classpath and causes
  // "Could not find or load main class org.openjdk.jmh.runner.ForkedMain" error

  def `run msgpack benchmark`: Unit = {
    BenchmarkMain.main(s"bench-quick -F 0 msgpack")
    warn(s"This is a test run result, and it may not reflect the actual performance")
  }

  def `run json benchmark`: Unit = {
    BenchmarkMain.main(s"bench-quick -F 0 json")
    warn(s"This is a test run result, and it may not reflect the actual performance")
  }

  def `run json-stream benchmark`: Unit = {
    BenchmarkMain.main(s"bench-quick -F 0 json_stream")
    warn(s"This is a test run result, and it may not reflect the actual performance")
  }

  def `run json perf benchmark`: Unit = {
    BenchmarkMain.main(s"json-perf -n ${iteration} -b ${iteration}")
  }

  def `run http benchmark`: Unit = {
    BenchmarkMain.main("bench-quick -F 0 -mt 2s http")
  }
}
