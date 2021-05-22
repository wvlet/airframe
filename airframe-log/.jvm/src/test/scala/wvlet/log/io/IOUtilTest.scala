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
  test("find unused port") {
    val port = IOUtil.unusedPort
    assert(port > 0)
  }

  test("find a file") {
    val buildSbt = IOUtil.findPath("build.sbt")
    assert(buildSbt.isDefined)
    assert(buildSbt.get.getPath == "build.sbt")

    val notFound = IOUtil.findPath("non-existing-file-path.xxxxxxx")
    assert(notFound.isEmpty)
  }

  test("read file as a String") {
    val str = IOUtil.readAsString("build.sbt")
    assert(str.length > 0)
  }

  test("throw FileNotFoundException if file is not found") {
    intercept[FileNotFoundException] {
      IOUtil.readAsString("non-existing-file-path.txt.tmp")
    }
  }
}
