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
package wvlet.log.io

import java.io.FileNotFoundException

import wvlet.log.Spec

/**
  */
class IOUtilTest extends Spec {
  def `find unused port`: Unit = {
    val port = IOUtil.unusedPort
    assert(port > 0)
  }

  def `find a file`: Unit = {
    val buildSbt = IOUtil.findPath("build.sbt")
    assert(buildSbt.isDefined)
    assert(buildSbt.get.getPath == "build.sbt")

    val notFound = IOUtil.findPath("non-existing-file-path.xxxxxxx")
    assert(notFound.isEmpty)
  }

  def `read file as a String`: Unit = {
    val str = IOUtil.readAsString("build.sbt")
    assert(str.length > 0)
  }

  def `throw FileNotFoundException if file is not found`: Unit = {
    intercept[FileNotFoundException] {
      IOUtil.readAsString("non-existing-file-path.txt.tmp")
    }
  }
}
