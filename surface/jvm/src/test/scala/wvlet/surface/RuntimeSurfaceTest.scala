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

package wvlet.surface

import javax.annotation.{PreDestroy, Resource}

@Resource(name = "annot-test")
class RuntimeAnnot(@Resource(name = "param A") a:String, c:Int) {

  @PreDestroy
  def b(@Resource(name = "b arg") arg:String) : String = "hello"

  def noAnnot : Int = 1
}


import wvlet.surface.runtime._
/**
  *
  */
class RuntimeSurfaceTest extends SurfaceSpec {
  "RuntimeSurface" should {
    "find class annotations" in {
      val r = Surface.of[RuntimeAnnot]
      val a = r.findAnnotationOf[Resource]
      a shouldBe defined
      a.get.name() shouldBe "annot-test"
    }

    "find parameter annotations" in {
      val s = Surface.of[RuntimeAnnot]
      val p = s.params.find(_.name == "a").get
      val a = p.findAnnotationOf[Resource]
      a shouldBe defined
      a.get.name() shouldBe "param A"

      val c = s.params.find(_.name == "c").get
      c.findAnnotationOf[Resource] shouldBe empty
    }

    "find method annotations" in {
      val m = Surface.methodsOf[RuntimeAnnot].find(_.name == "b").get
      val a = m.findAnnotationOf[PreDestroy]
      a shouldBe defined
      m.findAnnotationOf[Resource] shouldNot be (defined)

      val p = m.args.find(_.name == "arg").get
      val r = p.findAnnotationOf[Resource]
      r shouldBe defined
      r.get.name() shouldBe "b arg"
    }

    "pass sanity check" in {
      val m = Surface.methodsOf[RuntimeAnnot].find(_.name == "noAnnot").get
      m.annotations shouldBe empty
    }
  }
}
