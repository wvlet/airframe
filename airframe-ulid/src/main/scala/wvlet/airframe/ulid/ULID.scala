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

final case class ULID(private val ulid: String) {

  /**
    * Return the string representation of this ULID
    * @return
    */
  override def toString: String = ulid
  lazy val epochMillis: Long = {
    ULID.extractEpochMillis(ulid).getOrElse {
      throw new IllegalArgumentException(s"Invalid ULID")
    }
  }

  def toInstant: Instant = {
    Instant.ofEpochMilli(epochMillis)
  }
}

/**
  * ULID generator implementation based on https://github.com/petitviolet/ulid4s
  */
object ULID {
  private val defaultGenerator = {
    val timeSource = () => System.currentTimeMillis()
    val randGen = { () =>
      Random.nextDouble()
    }
    new ULIDGenerator(timeSource, randGen)
  }

  def newULID: ULID         = new ULID(defaultGenerator.generate)
  def newULIDString: String = defaultGenerator.generate

  def fromString(ulid: String): ULID = {
    if (isValid(ulid)) {
      new ULID(ulid)
    } else {
      throw new IllegalArgumentException(s"Invalid string for ULID: ${ulid}")
    }
  }

  /**
    * check a given string is valid as ULID
    * @param ulid
    * @return
    */
  def isValid(ulid: String): Boolean = {
    if (ulid.length != ULID_LENGTH) false
    else ulid.forall { CrockfordBase32.decode(_) != -1 }
  }

  /**
    * Extract epoch milliseconds (milliseconds from 1970-01-01 UTC) from the given ULID string
    * @param ulid ULID string
    * @return Some(timestamp) when given string is valid ULID, otherwise None
    */
  def extractEpochMillis(ulid: String): Option[Long] = {
    if (isValid(ulid)) {
      val result = ulid.take(10).reverse.zipWithIndex.foldLeft(0L) { case (acc, (c, index)) =>
        val idx = CrockfordBase32.indexOf(c)
        acc + (idx * Math.pow(ENCODING_LENGTH, index)).toLong
      }
      Option(result)
    } else None
  }

  private val ENCODING_LENGTH = 32

  private val TIMESTAMP_LENGTH       = 10
  private val RANDOM_LENGTH          = 16
  private[ulid] val ULID_LENGTH: Int = TIMESTAMP_LENGTH + RANDOM_LENGTH

  private[ulid] val MIN_TIME = 0x0L
  private[ulid] val MAX_TIME = 0x0000ffffffffffffL

  /**
    * ULID generator
    * @param timeSource a function returns the current time in milliseconds (e.g. java.lang.System.currentTimeMillis())
    * @param random a function returns a random value (e.g. scala.util.Random.nextDouble())
    */
  private[ulid] class ULIDGenerator(timeSource: () => Long, random: () => Double) {

    /**
      * generate ULID string
      * @return
      */
    def generate: String = encodeTime() + encodeRandom()

    private def encodeTime(): String = {
      @annotation.tailrec
      def run(time: Long, out: String = "", count: Int = 0): String = {
        count match {
          case TIMESTAMP_LENGTH => out
          case _ =>
            val mod = (time % ENCODING_LENGTH).toInt
            run((time - mod) / ENCODING_LENGTH, s"${CrockfordBase32.encode(mod)}${out}", count + 1)
        }
      }

      timeSource() match {
        case time if (time < MIN_TIME) || (MAX_TIME < time) =>
          throw new IllegalArgumentException(s"cannot generate ULID string. Time($time) is invalid");
        case time =>
          run(time)
      }
    }

    private def encodeRandom(): String = {
      @annotation.tailrec
      def run(out: String = "", count: Int = 0): String = {
        count match {
          case RANDOM_LENGTH => out
          case _ =>
            val rand = random()
            if (rand < 0.0d || 1.0d < rand) {
              throw new IllegalArgumentException(s"random must not under 0.0 or over 1.0. random value = $rand")
            }
            val index = Math.floor((ENCODING_LENGTH - 1) * rand).toInt
            run(s"${CrockfordBase32.encode(index)}${out}", count + 1)
        }
      }
      run()
    }
  }

}
