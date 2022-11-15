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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}
import scala.util.Random

/**
  * ULID string, consisting of 26 characters.
  */
final class ULID(private val ulid: String) extends Ordered[ULID] {

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

  /**
    * Return 48-bit UNIX-time of this ULID in milliseconds
    */
  def timestamp: Long = epochMillis

  /**
    * Return 80-bits randomness value of this ULID using a pair of (Long (16-bit), Long (64-bit))
    */
  def randomness: (Long, Long) = {
    val (hi, low) = CrockfordBase32.decode128bits(ulid)
    (hi & 0xffffL, low)
  }

  def toInstant: Instant = {
    Instant.ofEpochMilli(epochMillis)
  }

  /**
    * Get a 128-bit (16 byte) binary representation of this ULID.
    */
  def toBytes: Array[Byte] = {
    val (hi, low) = CrockfordBase32.decode128bits(ulid)
    val b         = new Array[Byte](16)
    for (i <- 0 until 8) {
      b(i) = ((hi >>> (64 - (i + 1) * 8)) & 0xffL).toByte
    }
    for (i <- 0 until 8) {
      b(i + 8) = ((low >>> (64 - (i + 1) * 8)) & 0xffL).toByte
    }
    b
  }

  override def compare(that: ULID): Int = {
    this.ulid.compareTo(that.ulid)
  }

  override def hashCode(): Int = ulid.hashCode

  override def equals(other: Any): Boolean = other match {
    case that: ULID =>
      ulid == that.ulid
    case _ => false
  }
}

/**
  * ULID generator implementation based on https://github.com/petitviolet/ulid4s
  *
  * ULID has 128 bit value: `|-- Unix timestamp milliseconds (48-bit) ---- | ----- random value (80 bits) ------ |`
  *
  * The string representation of ULID uses 26 characters in Crockford Base 32 representation, each character of which
  * represents 5-bit value (0-31).
  */
object ULID {
  val MaxValue: ULID        = ULID("7ZZZZZZZZZZZZZZZZZZZZZZZZZ")
  private[ulid] val MinTime = 0L
  private[ulid] val MaxTime = (~0L) >>> (64 - 48) // Timestamp uses 48-bit range

  private var _generator: ULIDGenerator = defaultULIDGenerator

  /**
    * The default secure random-based ULID Generator
    */
  def defaultULIDGenerator: ULIDGenerator = {
    val random: scala.util.Random = compat.random
    val randGen = { () =>
      val r = new Array[Byte](10)
      random.nextBytes(r)
      r
    }
    new ULIDGenerator(randGen)
  }

  /**
    * Return a fast ULID generator, but with a reduced randomness
    */
  def nonSecureRandomULIDGenerator: ULIDGenerator = {
    val randGen = { () =>
      val r = new Array[Byte](10); Random.nextBytes(r); r
    }
    new ULIDGenerator(randGen)
  }

  /**
    * Set the default ULIDGenerator that will be used for ULID.newULID.
    * @param newGenerator
    */
  def setDefaultULIDGenerator(newGenerator: ULIDGenerator): Unit = {
    require(newGenerator != null, "ULIDGenerator is null")
    _generator = newGenerator
  }

  /**
    * Use the fast ULID generator by default with reduced randomness
    */
  def useNonSecureRandomULIDGenerator: Unit = {
    setDefaultULIDGenerator(nonSecureRandomULIDGenerator)
  }

  /**
    * Use the default secura-random based ULID generator.
    */
  def useDefaultULIDGenerator: Unit = {
    setDefaultULIDGenerator(defaultULIDGenerator)
  }

  /**
    * Create a new ULID
    */
  def newULID: ULID = _generator.newULID

  /**
    * Create a new ULID string
    */
  def newULIDString: String = _generator.newULIDString

  /**
    * Create a new ULID from a given unix time in milli seconds
    * @param unixTimeMillis
    * @return
    */
  def ofMillis(unixTimeMillis: Long): ULID = {
    ULID(_generator.newULIDFromMillis(unixTimeMillis))
  }

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
    * Create an ULID from a given timestamp (48-bit) and a random value (80-bit)
    * @param unixTimeMillis
    *   48-bit unix time millis
    * @param randHi
    *   16-bit hi-part of 80-bit random value
    * @param randLow
    *   64-bit low-part of 80-bit random value
    * @return
    */
  def of(unixTimeMillis: Long, randHi: Long, randLow: Long): ULID = {
    if (unixTimeMillis < 0L || unixTimeMillis > MaxTime) {
      throw new IllegalArgumentException(f"unixtime must be between 0 to ${MaxTime}%,d: ${unixTimeMillis}%,d")
    }
    val hi: Long  = (unixTimeMillis << (64 - 48)) | (randHi & 0xffff)
    val low: Long = randLow
    new ULID(CrockfordBase32.encode128bits(hi, low))
  }

  /**
    * Create a ne ULID from a byte sequence (16-bytes)
    */
  def fromBytes(bytes: Array[Byte]): ULID = fromBytes(bytes, 0)

  /**
    * Create a ne ULID from a byte sequence (16-bytes)
    */
  def fromBytes(bytes: Array[Byte], offset: Int): ULID = {
    require(offset + 16 <= bytes.length, s"ULID needs 16 bytes. offset:${offset}, size:${bytes.length}")
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
    * ULID generator.
    * @param timeSource
    *   a function that returns the current time in milliseconds (e.g. java.lang.System.currentTimeMillis())
    * @param random
    *   a function that returns a 80-bit random values in Array[Byte] (size:10)
    */
  class ULIDGenerator(random: () => Array[Byte]) {
    private val baseSystemTimeMillis = System.currentTimeMillis()
    private val baseNanoTime         = System.nanoTime()

    private val lastValue = new AtomicReference((0L, 0L))

    private def currentTimeInMillis: Long = {
      // Avoid unexpected rollback of the system clock
      baseSystemTimeMillis + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - baseNanoTime)
    }

    def newULID: ULID = new ULID(newULIDString)

    /**
      * Generate a new ULID string.
      *
      * Tips for optimizing performance:
      *
      *   1. Reduce the number of Random number generation. SecureRandom is quite slow, so within the same milliseconds,
      *      just incrementing the randomness part will provide better performance. 2. Generate random in Array[Byte]
      *      (10 bytes = 80 bits). Regular Random uses 48-bit seed, so calling Random.nextInt (32 bits) x 3 is faster,
      *      but SecureRandom has optimization for Array[Byte] generation, which is much faster than calling nextInt
      *      three times. 3. ULIDs are often used in the string value form (e.g., transaction IDs, object IDs which can
      *      be embedded to URLs, etc.). Generating ULID String from the beginning is ideal. 4. In base32
      *      encoding/decoding, use bit-shift operators as much as possible to utilize CPU registers and memory cache.
      */
    def newULIDString: String = {
      val unixTimeMillis: Long = currentTimeInMillis
      newULIDFromMillis(unixTimeMillis)
    }

    def newULIDFromMillis(unixTimeMillis: Long): String = {
      if (unixTimeMillis > MaxTime) {
        throw new IllegalStateException(f"unixtime should be less than: ${MaxTime}%,d: ${unixTimeMillis}%,d")
      }
      // Add a guard so that only a single-thread can generate ULID based on the previous value
      synchronized {
        val (hi, low)    = lastValue.get()
        val lastUnixTime = (hi >>> 16) & 0xffffffffffffL
        if (lastUnixTime == unixTimeMillis) {
          // do increment
          if (low != ~0L) {
            generateFrom(hi, low + 1L)
          } else {
            var nextHi = (hi & ~(~0L << 16)) + 1
            if ((nextHi & (~0L << 16)) != 0) {
              // Random number overflow. Wait for one millisecond and retry
              compat.sleep(1)
              newULIDString
            } else {
              nextHi |= unixTimeMillis << (64 - 48)
              generateFrom(nextHi, 0)
            }
          }
        } else {
          // No conflict at millisecond level. We can generate a new ULID safely
          generateFrom(unixTimeMillis, random())
        }
      }
    }

    private def generateFrom(unixTimeMillis: Long, rand: Array[Byte]): String = {
      // We need a 80-bit random value here.
      require(rand.length == 10, s"random value array must have length 10, but ${rand.length}")

      val hi = ((unixTimeMillis & 0xffffffffffffL) << (64 - 48)) |
        (rand(0) & 0xffL) << 8 | (rand(1) & 0xffL)
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
      lastValue.set((hi, low))
      CrockfordBase32.encode128bits(hi, low)
    }
  }

}
