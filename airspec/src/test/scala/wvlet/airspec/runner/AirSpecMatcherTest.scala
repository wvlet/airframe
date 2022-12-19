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
package wvlet.airspec.runner

import wvlet.airspec.AirSpec

class AirSpecMatcherTest extends AirSpec {

  private val t1 = "hello airspec"
  private val t2 = "hello airspec/nested test"
  private val t3 = "hello airspec/nested test/A"
  private val t4 = "hello airspec/nested test/B"
  private val s1 = "different spec"
  private val s2 = "different spec/nested test"
  private val s3 = "different spec/nested test/A"
  private val s4 = "different spec/nested test/B"

  private val specs: List[String] = List(
    t1,
    t2,
    t3,
    t4,
    s1,
    s2,
    s3,
    s4
  )

  test("match all patterns") {
    val m = AirSpecMatcher.all
    specs.filter(m.matchWith(_)) shouldBe List(t1, t2, t3, t4, s1, s2, s3, s4)
  }

  test("match only first spec name") {
    val m = new AirSpecMatcher("hello")
    specs.filter(m.matchWith(_)) shouldBe List(t1, t2, t3, t4)
  }

  test("match only first spec name with a different name") {
    val m = new AirSpecMatcher("different")
    specs.filter(m.matchWith(_)) shouldBe List(s1, s2, s3, s4)
  }

  test("match until child spec name") {
    val m = new AirSpecMatcher("hello/nested")
    specs.filter(m.matchWith(_)) shouldBe List(t1, t2, t3, t4)
  }

  test("match until descendant spec name A") {
    val m = new AirSpecMatcher("hello/nested/A")
    specs.filter(m.matchWith(_)) shouldBe List(t1, t2, t3)
  }

  test("match until descendant spec name B") {
    val m = new AirSpecMatcher("hello/nested/B")
    specs.filter(m.matchWith(_)) shouldBe List(t1, t2, t4)
  }

  test("skip when only a middle test name is given") {
    val m = new AirSpecMatcher("nested")
    specs.filter(m.matchWith(_)) shouldBe empty
  }

}
