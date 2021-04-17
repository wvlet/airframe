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
class CrockfordBase32Test extends AirSpec with PropertyCheck {
  test("Encode long") {
    forAll { (hi: Long, low: Long) =>
      val encoded       = CrockfordBase32.encode128bits(hi, low)
      val (hi_d, low_d) = CrockfordBase32.decode128bits(encoded)
      (hi, low) shouldBe (hi_d, low_d)
    }
  }

}
