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

import wvlet.airframe.ulid.ULID.ULIDGenerator

import java.util.Calendar
import wvlet.airspec.AirSpec
import wvlet.airspec.spi.PropertyCheck

/**
  */
class ULIDCalendarTest extends AirSpec with PropertyCheck {
  private def ulid(timestamp: => Long, random: => Int) = {
    new ULIDGenerator(() => timestamp, () => random)
  }

  test("timestamp valid") {
    import org.scalacheck.Gen
    forAll(Gen.calendar) { cal: Calendar =>
      if (
        cal.getTimeInMillis > ULID.MinTime
        && cal.getTimeInMillis < ULID.MaxTime
      ) {
        val u = ULID(ulid(cal.getTimeInMillis, 0).generate)
        u.epochMillis shouldBe cal.getTimeInMillis
      }
    }
  }
}
