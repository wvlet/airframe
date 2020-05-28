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
    check("123M", Count(123000000L, Count.MILLION), "123M")

    check("123", Count(123, Count.ONE), "123")
    check("1234", Count(1234, Count.ONE), "1,234")
    check("2000", Count(2000, Count.ONE), "2,000")

    check("2K", Count(2000, Count.THOUSAND), "2K")
    check("10B", Count(10000000000L, Count.BILLION), "10B")
    check("1.23B", Count(1230000000L, Count.BILLION), "1.23B")
  }

  test("parse count strings with fraction") {
    check("1.2K", Count(1200, Count.THOUSAND), "1.20K")
    check("1.2M", Count(1200000L, Count.MILLION), "1.20M")
    check("1.234M", Count(1234000L, Count.MILLION), "1.23M")
    check("1.235M", Count(1235000L, Count.MILLION), "1.24M")
    check("123.4M", Count(123400000L, Count.MILLION), "123.40M")
  }
}
