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
  */
object ULID {
  val MaxValue: ULID        = ULID("7ZZZZZZZZZZZZZZZZZZZZZZZZZ")
  private[ulid] val MinTime = 0L
  private[ulid] val MaxTime = ((~0L) >>> (64 - 48)) // Timestamp uses 48-bit range

  private val ENCODING_LENGTH  = 32
  private val TIMESTAMP_LENGTH = 10
  private val RANDOM_LENGTH    = 16

  private val defaultGenerator = {
    val timeSource = () => System.currentTimeMillis()
    val randGen = { () => Random }
    new ULIDGenerator(timeSource, randGen)
  }

  def newULID: ULID         = new ULID(defaultGenerator.generate)
  def newULIDString: String = defaultGenerator.generate

  def apply(ulidString: String): ULID = fromString(ulidString)
  def fromString(ulid: String): ULID  = {
    require(ulid.length == 26, s"ULID must have 26 characters: ${ulid} (length: ${ulid.length})")
    require(CrockfordBase32.isValidBase32(ulid), s"Invalid Base32 character is found in ${ulid}")
    new ULID(ulid)
  }

  /**
    * check a given string is valid as ULID
    * @param ulid
    * @return
    */
  def isValid(ulid: String): Boolean = {
    ulid.length == 26 && CrockfordBase32.isValidBase32(ulid)
  }

  def from(unixTimeMillis:Long, randHi: Long, randLow: Long): String = {
    val hi: Long = (unixTimeMillis << (64 - 48)) |
            (randHi >>> (64 - 16) & 0xFFFFL)

    val low: Long = (randHi << 16) | (randLow <<

    CrockfordBase32.encode128bits(hi, low)
  }

  /**
    * ULID generator
    * @param timeSource a function returns the current time in milliseconds (e.g. java.lang.System.currentTimeMillis())
    * @param random a function returns a random value (e.g. scala.util.Random.nextDouble())
    */
  private[ulid] class ULIDGenerator(timeSource: () => Long, random: () => Random) {

    /**
      * generate ULID string
      * @return
      */
    def generate: String = {
      val unixTimeMillis: Long = timeSource()
      // 80-bits
      val rand = new Array[Byte](10)
      random().nextBytes(rand)
      val hi: Long = (unixTimeMillis << (64 - 48)) |
        (rand(0) & 0xffL) << 8 |
        (rand(1) & 0xffL)
      val low: Long = ((rand(2) & 0xffL) << 56) |
        ((rand(3) & 0xffL) << 48) |
        ((rand(4) & 0xffL) << 40) |
        ((rand(5) & 0xffL) << 32) |
        ((rand(6) & 0xffL) << 24) |
        ((rand(7) & 0xffL) << 16) |
        ((rand(8) & 0xffL) << 8) |
        (rand(9) & 0xffL)

      CrockfordBase32.encode128bits(hi, low)
    }
  }

}
