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

import wvlet.airspec.AirSpec

//--------------------------------------
//
// ResourceTest.scala
// Since: 2012/07/17 12:06
//
//--------------------------------------

class ResourceTest extends AirSpec {
  def `find files from the current class loader`: Unit = {
    debug("find files from package")
    val l = Resource.listResources("wvlet.log.io", { s: String => s.endsWith(".class") })
    assert(l.size > 0)
  }

  def `find resources from jar files`: Unit = {
    info("find files from a jar file")

    val l = Resource.listResources("scala.io", { s: String => s.endsWith(".class") })
    assert(l.size > 0)
    for (each <- l) {
      info(each)
      assert(each.url.toString.contains("/scala/io"))
    }
  }

  def `find classes of specific types`: Unit = {
    val l = Resource.findClasses("scala.io", classOf[scala.io.Source])
    assert(l.size > 0)
    debug(l)
    for (each <- l) {
      debug(each)
      assert(classOf[scala.io.Source].isAssignableFrom(each))
    }
  }

  def `find files using the context class`: Unit = {
    new ResourceReader {
      open("hello.txt") { f =>
        val lines = IOUtil.readAsString(f).split("\n")
        assert(lines.length == 1)
        assert(lines(0).toString == "Hello World!")
      }
    }
  }
}
