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

object MultipleConstructorArgsTest {
  case class MultiC(a: Int)(implicit val s: String) {
    def msg: String = s"${a}:${s}"
  }
}

import MultipleConstructorArgsTest._
class MultipleConstructorArgsTest extends SurfaceSpec {
  scalaJsSupport

  test("support muliple constructor args") {
    val s: Surface = Surface.of[MultiC]
    assert(s.objectFactory.nonEmpty)

    val f = s.objectFactory.get
    assert(s.params.size == 2)
    val i = f.newInstance(Seq(1, "hello"))
    assert(i.asInstanceOf[MultiC].msg == "1:hello")
  }
}
