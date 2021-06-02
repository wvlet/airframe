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
import wvlet.airspec.AirSpec

/**
  */
object AliasSurfaceTest extends AirSpec {

  case class Holder[A](v: A)
  type MyInt = Holder[Int]
  case class Component(rx: MyInt)

  test("inject an alias to parameterized types into constructors") {
    val s = Surface.of[Component]
    info(s)
  }
}
