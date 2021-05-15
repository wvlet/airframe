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

package wvlet.airframe.surface.reflect

import javax.annotation.PreDestroy
import wvlet.airframe.surface.{Surface, SurfaceSpec}

@Resource(name = "annot-test")
class RuntimeAnnot(@Resource(name = "param A") a: String, c: Int) {
  @PreDestroy
  def b(@Resource(name = "b arg") arg: String): String = "hello"

  def noAnnot: Int = 1
}

/**
  */
class RuntimeAnnotationTest extends SurfaceSpec {
  test("find class annotations") {
    val r = Surface.of[RuntimeAnnot]
    val a = r.findAnnotationOf[Resource]
    assert(a.isDefined)
    assert(a.get.name() == "annot-test")
  }

  test("find parameter annotations") {
    val s = Surface.of[RuntimeAnnot]
    val p = s.params.find(_.name == "a").get
    val a = p.findAnnotationOf[Resource]
    assert(a.isDefined)
    assert(a.get.name() == "param A")

    val c = s.params.find(_.name == "c").get
    assert(c.findAnnotationOf[Resource].isEmpty)
  }

  test("find method annotations") {
    val m = Surface.methodsOf[RuntimeAnnot].find(_.name == "b").get
    val a = m.findAnnotationOf[PreDestroy]
    assert(a.isDefined)
    assert(m.findAnnotationOf[Resource].isEmpty)

    val p = m.args.find(_.name == "arg").get
    debug(s"p: ${p}, ${p.index}")
    val r = p.findAnnotationOf[Resource]
    assert(r.isDefined)
    assert(r.get.name() == "b arg")
  }

  test("pass sanity check") {
    val m = Surface.methodsOf[RuntimeAnnot].find(_.name == "noAnnot").get
    assert(m.annotations.isEmpty)
  }
}
