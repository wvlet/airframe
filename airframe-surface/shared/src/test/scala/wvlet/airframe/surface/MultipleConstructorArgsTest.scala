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
package wvlet.airframe.surface
import wvlet.airframe.surface

object MultipleConstructorArgsTest {

  case class MultiC(a: Int)(implicit val s: String) {
    def msg: String = s"${a}:${s}"
  }

}

import MultipleConstructorArgsTest._
class MultipleConstructorArgsTest extends SurfaceSpec {
  "support muliple constructor args" in {

    val s: Surface = surface.of[MultiC]
    s.objectFactory shouldNot be(empty)

    val f = s.objectFactory.get
    s.params.size shouldBe 2
    val i = f.newInstance(Seq(1, "hello"))
    i.asInstanceOf[MultiC].msg shouldBe "1:hello"
  }
}
