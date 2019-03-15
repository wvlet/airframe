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
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import wvlet.airframe.launcher.{Launcher, command, option}
import wvlet.log.LogSupport

/**
  *
  */
object MsgpackBenchmarkMain {
  def main(args: Array[String]): Unit = {
    wvlet.airframe.log.init
    Launcher.of[MsgpackBenchmarkMain].execute(args)
  }
}

class MsgpackBenchmarkMain(
    @option(prefix = "-h,--help", description = "display help message", isHelp = true)
    displayHelp: Boolean)
    extends LogSupport {

  @command(isDefault = true)
  def default = {
    info("Type --help to see the list of sub commands")
  }

  @command(description = "Run a benchmark")
  def bench(@option(prefix = "-i,--iteration", description = "The number of iteration (default: 5)")
            iteration: Int = 5,
            @option(prefix = "-w,--warmup", description = "The number of warm-up iteration (default: 5)")
            warmupIteration: Int = 5,
            @option(prefix = "-f,--fork-count", description = "Fork Count (default: 5)")
            forkCount: Int = 5) {
    val opt = new OptionsBuilder()
      .forks(forkCount)
      .measurementIterations(iteration)
      .warmupIterations(warmupIteration)
      .include(".*" + classOf[MsgpackBenchmark].getSimpleName + ".*")
      .build()

    new Runner(opt).run()
  }
}
