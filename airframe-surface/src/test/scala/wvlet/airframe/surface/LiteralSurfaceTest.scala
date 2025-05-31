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
  * Test for literal surface handling issue
  */
class LiteralSurfaceTest extends AirSpec {
  test("reproduce the exact issue from the problem statement") {
    // Clear cache to ensure fresh state
    surfaceCache.clear()
    
    // Follow the exact sequence in the problem statement
    val literal1 = Surface.of[1] // Object
    val intSurface = Surface.of[Int] // Object
    val seqIntSurface = Surface.of[Seq[Int]]
    
    println(s"Surface.of[1]: ${literal1}")
    println(s"Surface.of[Int]: ${intSurface}")
    println(s"Surface.of[Seq[Int]].toString() == 'Seq[Object]': ${seqIntSurface.toString() == "Seq[Object]"}")
    
    // All of these return Object if run in this order
    val literal1_0 = Surface.of[1.0]
    val doubleSurface = Surface.of[Double]
    
    println(s"Surface.of[1.0]: ${literal1_0}")
    println(s"Surface.of[Double]: ${doubleSurface}")
    
    val literalChar = Surface.of['a']
    val charSurface = Surface.of[Char]
    
    println(s"Surface.of['a']: ${literalChar}")
    println(s"Surface.of[Char]: ${charSurface}")
    
    val literalBool = Surface.of[true]
    val boolSurface = Surface.of[Boolean]
    
    println(s"Surface.of[true]: ${literalBool}")
    println(s"Surface.of[Boolean]: ${boolSurface}")
  }
}