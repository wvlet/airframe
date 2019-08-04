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

import org.openjdk.jmh.results.format.ResultFormatType
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{OptionsBuilder, TimeValue}
import wvlet.airframe.launcher.{Launcher, command, option}
import wvlet.airframe.metrics.ElapsedTime
import wvlet.log.LogSupport

/**
  *
  */
object BenchmarkMain {
  wvlet.airframe.log.init

  private def launcher = Launcher.of[BenchmarkMain]

  def main(argLine: String): Unit = {
    launcher.execute(argLine)
  }

  def main(args: Array[String]): Unit = {
    launcher.execute(args)
  }
}

class BenchmarkMain(
    @option(prefix = "-h,--help", description = "display help message", isHelp = true)
    displayHelp: Boolean,
    @option(prefix = "-f", description = "Result format: text, csv, scsv, json, latex")
    resultFormat: Option[String] = None,
    @option(prefix = "-o", description = "Result output file name")
    resultOutput: Option[String] = None,
    @option(prefix = "-mt", description = "measurement time (default: 0.1s)")
    measurementTime: ElapsedTime = ElapsedTime.parse("0.1s"),
    @option(prefix = "-wt", description = "warmup time (default: 0.1s)")
    warmupTime: ElapsedTime = ElapsedTime.parse("0.1s")
) extends LogSupport {

  @command(isDefault = true)
  def default: Unit = {
    info("Type --help to see the list of sub commands")
  }

  @command(description = "Run a benchmark quickly")
  def bench_quick(
      @option(prefix = "-F,--fork-count", description = "Fork Count (default: 0)")
      forkCount: Int = 1
  ): Unit = {
    bench(iteration = 1, warmupIteration = 0, forkCount = forkCount)
  }

  @command(description = "Run a benchmark")
  def bench(@option(prefix = "-i,--iteration", description = "The number of iteration (default: 10)")
            iteration: Int = 10,
            @option(prefix = "-w,--warmup", description = "The number of warm-up iteration (default: 5)")
            warmupIteration: Int = 5,
            @option(prefix = "-F,--fork-count", description = "Fork Count (default: 5)")
            forkCount: Int = 5): Unit = {
    info("Starting the benchmark")
    var opt = new OptionsBuilder()
      .forks(forkCount)
      .measurementIterations(iteration)
      .warmupIterations(warmupIteration)
      .warmupTime(TimeValue.milliseconds(measurementTime.toMillis.toLong))
      .measurementTime(TimeValue.milliseconds(warmupTime.toMillis.toLong))
    //.include(".*" + classOf[MsgpackBenchmark].getSimpleName + ".*")

    resultFormat.map { rf =>
      opt = opt.resultFormat(ResultFormatType.valueOf(rf.toUpperCase()))
    }

    resultOutput.map { out =>
      opt = opt.result(out)
    }

    new Runner(opt.build()).run()
  }

  @command(description = "Run JSON benchmark")
  def json(@option(prefix = "-n", description = "The number of iteration (default: 10)")
           iteration: Int = 10,
           @option(prefix = "-b", description = "The number of block iteration (default: 10)")
           blockIteration: Int = 10,
  ): Unit = {
    JSONBenchmark.runAll(N = iteration, B = iteration)
  }

}
