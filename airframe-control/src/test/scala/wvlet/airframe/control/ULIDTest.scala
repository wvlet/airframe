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
package wvlet.airframe.control
import wvlet.airspec.AirSpec
import wvlet.airspec.spi.PropertyCheck

/**
  */
class ULIDTest extends AirSpec with PropertyCheck {

  private def ulid(timestamp: => Long, random: => Double) = {
    new ULIDGenerator(() => timestamp, () => random)
  }

  test("generate ULID") {
    for (i <- 0 to 10) {
      val ulid      = ULID.newULID
      val timestamp = ulid.epochMillis
      val str       = ulid.toString
      val parsed    = ULID.fromString(str)
      ulid shouldBe parsed
      debug(s"${ulid} ${timestamp} ${parsed}")
    }
  }

  test("valid") {
    ULID.isValid(ulid(System.currentTimeMillis(), 0.0d).generate) shouldBe true
  }

  test("generate") {
    ulid(ULID.constants.MIN_TIME, 0.0d).generate shouldBe "00000000000000000000000000"
    ulid(1L, 0.0d).generate shouldBe "00000000010000000000000000"
    ulid(ULID.constants.MAX_TIME, 0.0d).generate shouldBe "7ZZZZZZZZZ0000000000000000"

    ulid(0L, 0.5d).generate shouldBe "0000000000FFFFFFFFFFFFFFFF"
    ulid(0L, 1.0d).generate shouldBe "0000000000ZZZZZZZZZZZZZZZZ"
  }

  test("generation failures") {
    intercept[IllegalArgumentException] {
      ulid(ULID.constants.MIN_TIME - 1L, 0.0d).generate
    }
    intercept[IllegalArgumentException] {
      ulid(ULID.constants.MAX_TIME + 1L, 0.0d).generate
    }

    intercept[IllegalArgumentException] {
      ulid(0L, -0.1d).generate
    }
    intercept[IllegalArgumentException] {
      ulid(0L, 1.1d).generate
    }
  }

  test("invalid timestamp check") {
    forAll { str: String =>
      if (str.length != ULID.constants.ULID_LENGTH) {
        val result = ULID.extractEpochMillis(str)
        result shouldBe empty
      }
    }
  }
}
