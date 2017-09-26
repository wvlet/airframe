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

import wvlet.log.Spec

//--------------------------------------
//
// ResourceTest.scala
// Since: 2012/07/17 12:06
//
//--------------------------------------

class ResourceTest extends Spec {
  "Resource" should {
    "find files from the current class loader" in {
      debug("find files from package")
      val l = Resource.listResources("wvlet.log.io", { s: String =>
        s.endsWith(".class")
      })
      l.size should be > 0
    }

    "find resources from jar files" in {
      debug("find files from a jar file")

      val l = Resource.listResources("scala.io", { s: String =>
        s.endsWith(".class")
      })
      l.size should be > 0
      for (each <- l) {
        each.url.toString should include("/scala/io")
      }
    }

    "find classes of specific types" in {
      val l = Resource.findClasses("scala.io", classOf[scala.io.Source])
      l.size should be > 0
      debug(l)
      for (each <- l) {
        debug(each)
        classOf[scala.io.Source].isAssignableFrom(each) should be(true)
      }
    }
  }

  "ResourceReader trait" should {
    "find files using the context class" in {
      new ResourceReader {
        open("hello.txt") { f =>
          val lines = IOUtil.readAsString(f).split("\n")
          lines.length shouldBe 1
          lines(0).toString shouldBe "Hello World!"
        }
      }
    }
  }
}
