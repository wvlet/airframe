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

package wvlet.airframe.control
import scala.sys.process.Process

//--------------------------------------
//
// OSType.scala
// Since: 2012/01/30 11:58
//
//--------------------------------------

/**
  * OS type resolver
  *
  * @author leo
  */
object OS {
  def isWindows: Boolean = getType == OSType.Windows
  def isMac: Boolean     = getType == OSType.Mac
  def isLinux: Boolean   = getType == OSType.Linux
  lazy val isCygwin: Boolean = {
    Shell.findCommand("uname") match {
      case Some(uname) => Process(uname).!!.startsWith("CYGWIN")
      case None        => false
    }
  }

  def isUnix: Unit = {}

  val getType: OSType = {
    val osName: String = System.getProperty("os.name", "unknown").toLowerCase
    if (osName.contains("win")) {
      OSType.Windows
    } else if (osName.contains("mac")) {
      OSType.Mac
    } else if (osName.contains("linux")) {
      OSType.Linux
    } else
      OSType.Other
  }
}
