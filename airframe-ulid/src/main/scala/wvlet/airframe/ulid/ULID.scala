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
import scala.util.Random

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

  private val defaultGenerator = {
    val timeSource = () => System.currentTimeMillis()
    val randGen = { () => Random.nextInt() }
    new ULIDGenerator(timeSource, randGen)
  }

  def newULID: ULID         = new ULID(defaultGenerator.generate)
  def newULIDString: String = defaultGenerator.generate

  def apply(ulidString: String): ULID = fromString(ulidString)
  def fromString(ulid: String): ULID = {
    require(ulid.length == 26, s"ULID must have 26 characters: ${ulid} (length: ${ulid.length})")
    require(CrockfordBase32.isValidBase32(ulid), s"Invalid Base32 character is found in ${ulid}")
    new ULID(ulid)
  }
  def fromBytes(bytes: Array[Byte], offset: Int): ULID = {
    require(offset + 16 < bytes.length, s"ULID needs 16 bytes: ${offset + 16}")
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

  private def generateFrom(unixTimeMillis: Long, rand1: Int, rand2: Int, rand3: Int): String = {
    // We need a 80-bit random value. Use 32-bit * 3 = 96 bits
    val hi: Long  = (unixTimeMillis << (64 - 48)) | (rand1 & 0xffffL)
    val low: Long = (rand2.toLong << 32) | (rand3 & 0xffffffffL)

    CrockfordBase32.encode128bits(hi, low)
  }

  /**
    * ULID generator
    * @param timeSource a function returns the current time in milliseconds (e.g. java.lang.System.currentTimeMillis())
    * @param random a function returns a random value (e.g. scala.util.Random.nextDouble())
    */
  private[ulid] class ULIDGenerator(timeSource: () => Long, random: () => Int) {

    /**
      * generate ULID string
      * @return
      */
    def generate: String = {
      val unixTimeMillis: Long = timeSource()
      val r1                   = random()
      val r2                   = random()
      val r3                   = random()
      generateFrom(unixTimeMillis, r1, r2, r3)
    }
  }

}
