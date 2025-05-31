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
  * Test for the issue where Surface.of[Int] after Surface.of[1] returns Surface.of[Object]
  */
class LiteralTypeTest extends SurfaceSpec {
  test("literal types should not interfere with primitive types") {
    // Test the issue: Surface.of[1] followed by Surface.of[Int] returns Object
    val literal1 = Surface.of[1]
    val intSurface = Surface.of[Int]
    
    debug(s"Surface.of[1]: ${literal1} (class: ${literal1.getClass.getSimpleName})")
    debug(s"Surface.of[Int]: ${intSurface} (class: ${intSurface.getClass.getSimpleName})")
    
    // Surface.of[Int] should always return "Int", not "Object"
    intSurface.toString shouldBe "Int"
    
    // Test with Seq[Int] 
    val seqInt = Surface.of[Seq[Int]]
    debug(s"Surface.of[Seq[Int]]: ${seqInt}")
    seqInt.toString shouldBe "Seq[Int]"
  }
  
  test("double literal types should not interfere") {
    val literal1_0 = Surface.of[1.0]
    val doubleSurface = Surface.of[Double]
    
    debug(s"Surface.of[1.0]: ${literal1_0}")
    debug(s"Surface.of[Double]: ${doubleSurface}")
    
    doubleSurface.toString shouldBe "Double"
  }
  
  test("char literal types should not interfere") {
    val literalA = Surface.of['a']
    val charSurface = Surface.of[Char]
    
    debug(s"Surface.of['a']: ${literalA}")
    debug(s"Surface.of[Char]: ${charSurface}")
    
    charSurface.toString shouldBe "Char"
  }
  
  test("boolean literal types should not interfere") {
    val literalTrue = Surface.of[true]
    val booleanSurface = Surface.of[Boolean]
    
    debug(s"Surface.of[true]: ${literalTrue}")
    debug(s"Surface.of[Boolean]: ${booleanSurface}")
    
    booleanSurface.toString shouldBe "Boolean"
  }
}