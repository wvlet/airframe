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
  *
  */
class IOUtilTest extends Spec {

  "IOUtil" should {
    "find unused port" in {
      val port = IOUtil.unusedPort
      port shouldBe >(0)
    }

    "find a file" in {
      val buildSbt = IOUtil.findPath("build.sbt")
      buildSbt shouldBe defined
      buildSbt.get.getPath shouldBe "build.sbt"

      val notFound = IOUtil.findPath("non-existing-file-path.xxxxxxx")
      notFound shouldBe empty
    }

    "read file as a String" in {
      val str = IOUtil.readAsString("build.sbt")
      str.length shouldBe >(0)
    }

    "throw FileNotFoundException if file is not found" in {
      intercept[FileNotFoundException] {
        IOUtil.readAsString("non-existing-file-path.txt.tmp")
      }
    }
  }
}
