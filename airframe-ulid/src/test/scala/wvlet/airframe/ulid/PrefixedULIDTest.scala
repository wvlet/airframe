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
package wvlet.airframe.ulid

import wvlet.airspec.AirSpec
import wvlet.airspec.spi.PropertyCheck

/**
  */
class PrefixedULIDTest extends AirSpec with PropertyCheck {

  test("basic") {
    val ulid = ULID.newULID
    PrefixedULID(prefix = "abcde", ulid = ulid).toString shouldBe s"abcde:${ulid.toString}"
  }

  test("fromString") {
    val ulid               = ULID.newULID
    val prefixedULIDString = s"P:${ulid}"
    val prefixedULID       = PrefixedULID.fromString(prefixedULIDString)
    prefixedULID.prefix shouldBe "P"
    prefixedULID.ulid shouldBe ulid
  }

  test("Handle invalid strings for fromString") {
    intercept[IllegalArgumentException] {
      PrefixedULID.fromString(ULID.newULIDString)
    }
    intercept[IllegalArgumentException] {
      PrefixedULID.fromString("P:")
    }
  }

  test("Allow an empty prefix") {
    val ulid = ULID.newULID
    val p    = PrefixedULID.fromString(s":${ulid}")
    p.prefix shouldBe empty
    p.ulid shouldBe ulid
  }

  test("Generate new prefixed ULID") {
    val p = PrefixedULID.newPrefixedULID("P")
    p.prefix shouldBe "P"
    ULID.fromString(p.toString.stripPrefix("P:")) shouldBe p.ulid
  }

  test("Generate new prefixed ULID string") {
    val p = PrefixedULID.newPrefixedULIDString("P")
    p.startsWith("P:") shouldBe true
    // The suffix should be a valid ULID
    ULID.fromString(p.stripPrefix("P:"))
  }

  test("have an order") {
    val p1 = PrefixedULID.newPrefixedULID("A")
    val p2 = PrefixedULID.newPrefixedULID("B")
    p1 shouldBe p1
    p2 shouldBe p2
    p1 shouldNotBe p2
    p2 shouldNotBe p1

    p1.compare(p1) shouldBe 0
    p1.compare(p2) < 0 shouldBe true
    p2.compare(p1) > 0 shouldBe true

    p1 < p2 shouldBe true
    p2 > p1 shouldBe true
    p1 shouldNotBe p2
  }
  test("have an order with the same prefix values") {
    // Generates monotonically increasing PrefixedULID by default
    val p1 = PrefixedULID.newPrefixedULID("A")
    val p2 = PrefixedULID.newPrefixedULID("A")
    p1 shouldNotBe p2
    p1 < p2 shouldBe true
    p2 > p1 shouldBe true
  }
}
