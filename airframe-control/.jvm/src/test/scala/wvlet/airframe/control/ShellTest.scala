/*
 * Copyright 2012 Taro L. Saito
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.control
import wvlet.airspec.AirSpec

//--------------------------------------
//
// ShellTest.scala
// Since: 2012/02/06 13:04
//
//--------------------------------------

/**
  * @author leo
  */
class ShellTest extends AirSpec {

  def `find JVM`: Unit = {
    val j = Shell.findJavaCommand()
    j shouldBe defined
  }

  def `find javaw.exe`: Unit = {
    if (OS.isWindows) {
      val cmd = Shell.findJavaCommand("javaw").get
      cmd shouldNotBe null
      cmd.contains("javaw") shouldBe true
    }
  }

  def `detect process IDs`: Unit = {
    val p   = Shell.launchProcess("echo hello world")
    val pid = Shell.getProcessID(p)
    debug(s"process ID:$pid")
    if (!OS.isWindows) {
      pid > 0 shouldBe true
    }
  }

  def `detect current JVM process ID`: Unit = {
    val pid = Shell.getProcessIDOfCurrentJVM
    debug(s"JVM process ID:$pid")
    pid shouldNotBe -1
  }

  def `be able to launch Java`: Unit = {
    Shell.launchJava("-version -Duser.language=en")
  }

  def `be able to kill processes`: Unit = {
    val p        = Shell.launchProcess("cat")
    val pid      = Shell.getProcessID(p)
    val exitCode = Shell.kill(pid)
  }

  def `be able to kill process trees`: Unit = {
    val p   = Shell.launchProcess("cat")
    val pid = Shell.getProcessID(p)
    Shell.killTree(pid)
  }

  def `find sh`: Unit = {
    val cmd = Shell.findSh
    cmd shouldBe defined
  }

  def `launch command`: Unit = {
    Shell.launchProcess("echo hello world")
    Shell.launchProcess("echo cygwin env=$CYGWIN")
  }

  def `launch process`: Unit = {
    if (OS.isWindows) {
      Shell.launchCmdExe("echo hello cmd.exe")
    }
  }

  def `launch a remote process as a daemon`: Unit = {
    pending // disabled because ssh cannot be used in travis CI
    Shell.launchRemoteDaemon("localhost", "sleep 5")
  }

}
