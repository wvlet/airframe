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
    val ulid = ULID.of(ULID.MinTime, 0, 0)
    PrefixedULID(prefix = "abcde", ulid = ulid).toString shouldBe "abcde:00000000000000000000000000"
    PrefixedULID(prefix = "abcde", ulid = ulid, delimiter = '/').toString shouldBe "abcde/00000000000000000000000000"
  }
}
