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
package wvlet.airframe.metrics
import wvlet.airspec.AirSpec

/**
  *
 */
class CountTest extends AirSpec {

  private def check(s: String, expected: Count, expectedString: String) {
    val c = Count(s)
    c shouldBe expected
    c.toString shouldBe expectedString
  }

  test("parse count strings") {
    check("1M", Count(1000000L, Count.MILLION), "1M")
    check("123", Count(123, Count.ONE), "123")
    check("1234", Count(1234, Count.ONE), "1,234")
    check("1,234", Count(1234, Count.ONE), "1,234")
    check("1.2K", Count(1200, Count.THOUSAND), "1.20K")
    check("2000", Count(2000, Count.ONE), "2,000")
    check("2K", Count(2000, Count.THOUSAND), "2K")
  }

  test("parse count strings with fraction") {
    val c = Count("1.2M")
    c shouldBe Count(1200000L, Count.MILLION)
    c.toString shouldBe "1.20M"

    val c2 = Count("123M")
    c2 shouldBe Count(123000000L, Count.MILLION)
    c2.toString shouldBe "123M"
  }
}
