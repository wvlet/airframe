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

class CNameTest extends munit.FunSuite {
  test("convert to snakeCase") {
    assert(CName("AirframeSurface").snakeCase == "airframe_surface")
    assert(CName("airframe_surface").snakeCase == "airframe_surface")
    assert(CName("airframe-surface").snakeCase == "airframe_surface")
    assert(CName("airframeSurface").snakeCase == "airframe_surface")
    assert(CName("Airframe Surface").snakeCase == "airframe_surface")
  }

  test("convert to dashCase") {
    assert(CName("AirframeSurface").dashCase == "airframe-surface")
    assert(CName("airframe_surface").dashCase == "airframe-surface")
    assert(CName("airframe-surface").dashCase == "airframe-surface")
    assert(CName("airframeSurface").dashCase == "airframe-surface")
    assert(CName("Airframe Surface").dashCase == "airframe-surface")
  }

  test("convert to .upperCamelCase") {
    assert(CName("AirframeSurface").upperCamelCase == "AirframeSurface")
    assert(CName("airframe_surface").upperCamelCase == "AirframeSurface")
    assert(CName("airframe-surface").upperCamelCase == "AirframeSurface")
    assert(CName("airframeSurface").upperCamelCase == "AirframeSurface")
    assert(CName("Airframe Surface").upperCamelCase == "AirframeSurface")
  }

  test("convert to lowerCamelCase") {
    assert(CName("AirframeSurface").lowerCamelCase == "airframeSurface")
    assert(CName("airframe_surface").lowerCamelCase == "airframeSurface")
    assert(CName("airframe-surface").lowerCamelCase == "airframeSurface")
    assert(CName("airframeSurface").lowerCamelCase == "airframeSurface")
    assert(CName("Airframe Surface").lowerCamelCase == "airframeSurface")
  }

  test("null values") {
    val nullName = CName(null)
    assertEquals(nullName.canonicalName, "")
    assertEquals(nullName.naturalName, "")
  }

  test("comparison") {
    val a = CName("Apple")
    val b = CName("Banana")
    assertEquals(a.compareTo(b) < 0, true)
    assertEquals(a, a)
    assertNotEquals(a, b)
    assertNotEquals(a.hashCode, b.hashCode)
  }
}
