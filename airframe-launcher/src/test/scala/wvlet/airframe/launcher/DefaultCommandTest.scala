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
package wvlet.airframe.launcher

import wvlet.airspec.AirSpec
import wvlet.log.LogSupport
import wvlet.airframe.surface.command.*

class DefaultCommandTest extends AirSpec {
  test("options in default commands should be accessible") {
    val result = capture {
      Launcher.of[DefaultCommandExample].execute("-w", "custom_workdir")
    }
    result.contains("workDir: custom_workdir") shouldBe true
  }

  test("options in default commands with explicit name should be accessible") {
    val result = capture {
      Launcher.of[DefaultCommandExample].execute("repl", "-w", "custom_workdir")
    }
    result.contains("workDir: custom_workdir") shouldBe true
  }

  def capture[U](body: => U): String = {
    LauncherTest.capture(body)
  }
}

case class CliOption(
  @option(prefix = "-w")
  workDir: String = "default_workdir"
)

class DefaultCommandExample extends LogSupport {
  @command(isDefault = true)
  def repl(opts: CliOption): Unit = {
    debug(s"workDir: ${opts.workDir}")
  }
}