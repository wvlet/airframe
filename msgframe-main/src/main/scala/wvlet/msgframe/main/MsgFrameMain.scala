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

package wvlet.msgframe.main
import wvlet.airframe.launcher.{Launcher, defaultCommand, option}
import wvlet.log.LogSupport
import wvlet.msgframe.sql.SQLMain

object MsgFrameMain {

  def main(args: Array[String]): Unit = {
    val l = Launcher
      .of[MsgFrameMain]
      .addModule[SQLMain](name = "sql", description = "SQL analysis module")

    wvlet.airframe.log.init
    l.execute(args)
  }
}

/**
  *
  */
class MsgFrameMain(
    @option(prefix = "-h,--help", description = "Show help messages", isHelp = true)
    displayHelp: Boolean
) extends LogSupport {

  info(s"msgframe: version ${BuildInfo.version}")

  @defaultCommand
  def default: Unit = {
    info("Type --help to see the list sub commands")
  }
}
