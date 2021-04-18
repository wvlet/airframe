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

import java.util.Calendar
import scala.util.Random

/**
  */
class ULIDCalendarTest extends AirSpec with PropertyCheck {
  test("timestamp valid") {
    import org.scalacheck.Gen
    forAll(Gen.calendar) { cal: Calendar =>
      if (
        cal.getTimeInMillis > ULID.MinTime
        && cal.getTimeInMillis < ULID.MaxTime
      ) {
        val u = ULID.of(cal.getTimeInMillis, Random.nextLong(), Random.nextLong())
        u.epochMillis shouldBe cal.getTimeInMillis
      }
    }
  }
}
