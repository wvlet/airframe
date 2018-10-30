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
import wvlet.airframe.AirframeSpec

//--------------------------------------
//
// ShellTest.scala
// Since: 2012/02/06 13:04
//
//--------------------------------------

/**
  * @author leo
  */
class ShellTest extends AirframeSpec {
  "Shell" should {
    "find JVM" in {
      val j = Shell.findJavaCommand()
      j should be('defined)
    }

    "find javaw.exe" in {
      if (OS.isWindows) {
        When("OS is windows")
        val cmd = Shell.findJavaCommand("javaw").get
        cmd shouldNot be(null)
        cmd should include("javaw")
      }
    }

    "detect process IDs" in {
      val p   = Shell.launchProcess("echo hello world")
      val pid = Shell.getProcessID(p)
      debug(s"process ID:$pid")
      if (!OS.isWindows) {
        pid should be > (0)
      }
    }

    "detect current JVM process ID" in {
      val pid = Shell.getProcessIDOfCurrentJVM
      debug(s"JVM process ID:$pid")
      pid should not be (-1)
    }

    "be able to launch Java" in {
      Shell.launchJava("-version -Duser.language=en")
    }

    "be able to kill processes" in {
      val p        = Shell.launchProcess("cat")
      val pid      = Shell.getProcessID(p)
      val exitCode = Shell.kill(pid)
    }

    "be able to kill process trees" in {
      val p   = Shell.launchProcess("cat")
      val pid = Shell.getProcessID(p)
      Shell.killTree(pid)
    }

    "find sh" in {
      val cmd = Shell.findSh
      cmd should be('defined)
    }

    "launch command" in {
      Shell.launchProcess("echo hello world")
      Shell.launchProcess("echo cygwin env=$CYGWIN")
    }

    "launch process" taggedAs ("launch_process") in {
      if (OS.isWindows) {
        When("OS is windows")
        Shell.launchCmdExe("echo hello cmd.exe")
      }
    }

    "launch a remote process as a daemon" in {
      pending // disabled because ssh cannot be used in travis CI
      Shell.launchRemoteDaemon("localhost", "sleep 5")
    }
  }
}
