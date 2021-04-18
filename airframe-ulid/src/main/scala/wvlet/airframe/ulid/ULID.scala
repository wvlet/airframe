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
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

final case class ULID(private val ulid: String) extends Ordered[ULID] {

  /**
    * Return the string representation of this ULID
    * @return
    */
  override def toString: String = ulid

  /**
    * Get the epoch milliseconds (milliseconds from 1970-01-01 UTC) of this ULID.
    */
  lazy val epochMillis: Long = {
    CrockfordBase32.decode48bits(ulid.substring(0, 10))
  }

  def toInstant: Instant = {
    Instant.ofEpochMilli(epochMillis)
  }

  override def compare(that: ULID): Int = {
    this.ulid.compareTo(that.ulid)
  }
}

/**
  * ULID generator implementation based on https://github.com/petitviolet/ulid4s
  *
  * ULID has 128 bit value:
  * |-- Unix timestamp milliseconds (48-bit) ---- | -----  random value (80 bits) ------ |
  *
  * The string representation of ULID uses 26 characters in Crockford Base 32 representation,
  * each character of which represents 5-bit value (0-31).
  */
object ULID {
  val MaxValue: ULID        = ULID("7ZZZZZZZZZZZZZZZZZZZZZZZZZ")
  private[ulid] val MinTime = 0L
  private[ulid] val MaxTime = (~0L) >>> (64 - 48) // Timestamp uses 48-bit range

  private val random: scala.util.Random = compat.random

  private val defaultGenerator = {
    val timeSource = () => System.currentTimeMillis()
    val randGen = { () =>
      val r = new Array[Byte](10)
      random.nextBytes(r)
      r
    }
    new ULIDGenerator(timeSource, randGen)
  }

  /**
    * Create a new ULID
    */
  def newULID: ULID = new ULID(defaultGenerator.generate)

  /**
    * Create a new ULID string
    */
  def newULIDString: String = defaultGenerator.generate

  /**
    * Create a new ULID from a given string of size 26
    */
  def apply(ulidString: String): ULID = fromString(ulidString)

  /**
    * Create a new ULID from a given string of size 26
    */
  def fromString(ulid: String): ULID = {
    require(ulid.length == 26, s"ULID must have 26 characters: ${ulid} (length: ${ulid.length})")
    require(CrockfordBase32.isValidBase32(ulid), s"Invalid Base32 character is found in ${ulid}")
    new ULID(ulid)
  }

  /**
    * Create a ne ULID from a byte sequence (16-bytes)
    */
  def fromBytes(bytes: Array[Byte]): ULID = fromBytes(bytes, 0)

  /**
    * Create a ne ULID from a byte sequence (16-bytes)
    */
  def fromBytes(bytes: Array[Byte], offset: Int): ULID = {
    require(offset + 16 < bytes.length, s"ULID needs 16 bytes. offset:${offset}, size:${bytes.length}")
    var i  = 0
    var hi = 0L
    while (i < 8) {
      hi <<= 8
      hi |= bytes(offset + i) & 0xffL
      i += 1
    }
    var low = 0L
    while (i < 16) {
      low <<= 8
      low |= bytes(offset + i) & 0xffL
      i += 1
    }
    new ULID(CrockfordBase32.encode128bits(hi, low))
  }

  def unapply(ulidString: String): Option[ULID] = {
    if (isValid(ulidString)) {
      Some(new ULID(ulidString))
    } else {
      None
    }
  }

  /**
    * check a given string is valid as ULID
    * @param ulid
    * @return
    */
  def isValid(ulid: String): Boolean = {
    ulid.length == 26 && CrockfordBase32.isValidBase32(ulid)
  }

  /**
    * ULID generator
    * @param timeSource a function returns the current time in milliseconds (e.g. java.lang.System.currentTimeMillis())
    * @param random a function returns a random value (e.g. scala.util.Random.nextDouble())
    */
  private[ulid] class ULIDGenerator(timeSource: () => Long, random: () => Array[Byte]) {

    private val lastUnixTimeMillis = new AtomicLong(-1L)
    private val lastHi             = new AtomicLong(0L)
    private val lastLow            = new AtomicLong(0L)

    private def generateFrom(unixTimeMillis: Long, rand: Array[Byte]): String = {
      // We need a 80-bit random value here.
      require(rand.length == 10)

      val hi: Long = (unixTimeMillis << (64 - 48)) |
        (rand(0) & 0xffL << 8)
      (rand(1) & 0xffL)
      val low: Long =
        ((rand(2) & 0xffL) << 56) |
          ((rand(3) & 0xffL) << 48) |
          ((rand(4) & 0xffL) << 40) |
          ((rand(5) & 0xffL) << 32) |
          ((rand(6) & 0xffL) << 24) |
          ((rand(7) & 0xffL) << 16) |
          ((rand(8) & 0xffL) << 8) |
          (rand(9) & 0xffL)

      generateFrom(hi, low)
    }

    private def generateFrom(hi: Long, low: Long): String = {
      lastHi.set(hi)
      lastLow.set(low)
      CrockfordBase32.encode128bits(hi, low)
    }

    /**
      * generate ULID string
      * @return
      */
    def generate: String = {
      val unixTimeMillis: Long = timeSource()
      if (unixTimeMillis > MaxTime) {
        throw new IllegalStateException(f"unixtime should be less than: ${MaxTime}%,d: ${unixTimeMillis}%,d")
      }
      if (lastUnixTimeMillis.get() == unixTimeMillis) {
        val hi  = lastHi.get
        val low = lastLow.get
        // do increment
        if (low != ~0L) {
          generateFrom(hi, low + 1L)
        } else {
          var nextHi = (hi & ~(~0L << 16)) + 1
          if ((nextHi & (~0L << 16)) != 0) {
            // overflow. Wait one millisecond
            compat.sleep(1)
            generate
          } else {
            nextHi |= unixTimeMillis << (64 - 48)
            generateFrom(nextHi, 0)
          }
        }
      } else {
        lastUnixTimeMillis.set(unixTimeMillis)
        val rand = random()
        generateFrom(unixTimeMillis, rand)
      }
    }
  }

}
