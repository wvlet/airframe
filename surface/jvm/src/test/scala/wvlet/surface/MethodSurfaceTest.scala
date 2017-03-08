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

object MethodExamples {

  class A {
    def hello : String = "hello"
    def arg2(i:Int, b:Boolean) : String = "arg2"
    def abst[X](v:X) : X = v
    protected def helloProtected = "hello"
    private def helloPrivate = "hello"

    @PreDestroy
    def hasAnnotation : Unit = {}

    @Resource(name="my resource")
    def resource : Unit = {}
  }

  type MyA = A
}


import MethodExamples._

/**
  *
  */
class MethodSurfaceTest extends SurfaceSpec {

  "MethodSurface" should {

    "list methods" in {
      val m = Surface.methodsOf[A]
      info(m.mkString("\n"))

      val m2 = Surface.methodsOf[MyA]
      info(m2)
    }

  }
}
