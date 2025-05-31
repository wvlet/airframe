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

/**
  * Test for the specific behavior mentioned in the problem statement
  */
class SequenceTest extends SurfaceSpec {

  test("Seq[Int] should work correctly after literal type resolution") {
    // This mimics the exact sequence mentioned in the problem statement
    val surface1 = Surface.of[1]        // Object
    val surface2 = Surface.of[Int]      // Should be Int, not Object
    val surface3 = Surface.of[Seq[Int]] // Should be Seq[Int], not Seq[Object]

    debug(s"Surface.of[1]: ${surface1}")
    debug(s"Surface.of[Int]: ${surface2}")
    debug(s"Surface.of[Seq[Int]]: ${surface3}")

    surface2.toString shouldBe "Int"
    surface3.toString shouldBe "Seq[Int]"
  }
}
