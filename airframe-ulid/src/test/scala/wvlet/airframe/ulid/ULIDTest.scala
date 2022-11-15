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
class ULIDTest extends AirSpec with PropertyCheck {

  test("generate ULID") {
    for (i <- 0 to 10) {
      val ulid      = ULID.newULID
      val timestamp = ulid.epochMillis
      val str       = ulid.toString
      val parsed    = ULID.fromString(str)
      ulid shouldBe parsed
      ulid <= ULID.MaxValue shouldBe true
      debug(s"${ulid} ${timestamp} ${parsed}")
    }
  }

  test("toString/toBytes") {
    forAll { (a: Long, rh: Long, rl: Long) =>
      val unixTime = a & ((~0L) >>> (64 - 48))
      val ulid     = ULID.of(unixTime, rh, rl)
      // Identity
      ulid.compareTo(ulid) shouldBe 0
      ULID.fromString(ulid.toString) shouldBe ulid
      ULID.fromBytes(ulid.toBytes) shouldBe ulid

      // Basic conditions
      ulid.epochMillis shouldBe unixTime
      ulid.timestamp shouldBe unixTime
      ulid.randomness shouldBe (rh & 0xffffL, rl)
      ULID.isValid(ulid.toString) shouldBe true
    }
  }

  test("generate monotonically increasing ULIDs") {
    val lst: Seq[ULID] = (0 to 1000).map { i => ULID.newULID }
    lst.sliding(2).forall { pair =>
      pair(0) < pair(1) &&
      pair(0).epochMillis <= pair(1).epochMillis
    }
  }

  test("valid") {
    forAll { (timeMillis: Long) =>
      val unixtime = timeMillis & 0xffffffffffffL
      val ulid     = ULID.of(unixtime, 0, 0)
      ulid.epochMillis shouldBe unixtime
    }
  }

  test("generate") {
    ULID.of(ULID.MinTime, 0, 0) shouldBe ULID("00000000000000000000000000")
    ULID.of(1L, 0, 0) shouldBe ULID("00000000010000000000000000")
    ULID.of(ULID.MaxTime, 0, 0) shouldBe ULID("7ZZZZZZZZZ0000000000000000")
    ULID.of(ULID.MaxTime, ~0L, ~0L) shouldBe ULID.MaxValue
    ULID.of(0L, 0, ~0L) shouldBe ULID("0000000000000FZZZZZZZZZZZZ")
    ULID.of(0L, ~0L, ~0L) shouldBe ULID("0000000000ZZZZZZZZZZZZZZZZ")
  }

  test("generation failures") {
    intercept[IllegalArgumentException] {
      ULID.of(ULID.MinTime - 1L, 0, 0)
    }
    intercept[IllegalArgumentException] {
      ULID.of(ULID.MaxTime + 1L, 0, 0)
    }
  }

  test("equals/hashCode") {
    val ulid1 = ULID.newULID
    val ulid2 = ULID.fromString(ulid1.toString)

    ulid1 shouldBe ulid2
    ulid1.equals(ulid2) shouldBe true
    ulid1.equals(null) shouldBe false
    ulid1.hashCode() shouldBe ulid2.hashCode()
  }

  test("encode timestamp") {
    val ulid      = ULID.newULID
    val ts        = ulid.epochMillis
    val tsString  = ulid.toString.substring(0, 10)
    val decodedTs = CrockfordBase32.decode48bits(tsString)
    ts shouldBe decodedTs
  }

  test("customize random generator") {
    try {
      ULID.useNonSecureRandomULIDGenerator
      for (i <- 0 to 10) {
        val ulid      = ULID.newULID
        val timestamp = ulid.epochMillis
        val str       = ulid.toString
        val parsed    = ULID.fromString(str)
        ulid shouldBe parsed
        ulid <= ULID.MaxValue shouldBe true
        debug(s"${ulid} ${timestamp} ${parsed}")
      }
    } finally {
      ULID.useDefaultULIDGenerator
    }
  }

  test("generate ULID from a given unix time") {
    val unixTime = System.currentTimeMillis() - 10000
    val u        = ULID.ofMillis(unixTime)
    u.epochMillis shouldBe unixTime
  }
}
