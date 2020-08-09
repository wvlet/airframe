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
import wvlet.airframe.control.ULID.extractEpochMillis

import scala.util.Random

final case class ULID(ulid: String) {
  override def toString: String = ulid
  def epochMillis: Long = {
    extractEpochMillis(ulid).getOrElse {
      throw new IllegalArgumentException(s"Invalid ULID")
    }
  }
}

object ULID {
  private val defaultGenerator = {
    val timeSource = () => System.currentTimeMillis()
    val randGen = { () => Random.nextDouble() }
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
    if (ulid.length != constants.ULID_LENGTH) false
    else ulid.forall { constants.DECODING_CHARS(_) != -1 }
  }

  /**
    * Extract epoch milliseconds (milliseconds from 1970-01-01 UTC) from the given ULID string
    * @param ulid ULID string
    * @return Some(timestamp) when given string is valid ULID, otherwise None
    */
  def extractEpochMillis(ulid: String): Option[Long] = {
    if (isValid(ulid)) {
      val result = ulid.take(10).reverse.zipWithIndex.foldLeft(0L) {
        case (acc, (c, index)) =>
          val idx = constants.ENCODING_CHARS.indexOf(c)
          acc + (idx * Math.pow(constants.ENCODING_LENGTH, index)).toLong
      }
      Option(result)
    } else None
  }

  private[control] object constants {
    val ENCODING_CHARS: Array[Char] = Array(
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P',
      'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z'
    )

    val DECODING_CHARS: Array[Byte] = Array[Byte](
      -1, -1, -1, -1, -1, -1, -1, -1, // 0
      -1, -1, -1, -1, -1, -1, -1, -1, // 8
      -1, -1, -1, -1, -1, -1, -1, -1, // 16
      -1, -1, -1, -1, -1, -1, -1, -1, // 24
      -1, -1, -1, -1, -1, -1, -1, -1, // 32
      -1, -1, -1, -1, -1, -1, -1, -1, // 40
      0, 1, 2, 3, 4, 5, 6, 7,         // 48
      8, 9, -1, -1, -1, -1, -1, -1,   // 56
      -1, 10, 11, 12, 13, 14, 15, 16, // 64
      17, 1, 18, 19, 1, 20, 21, 0,    // 72
      22, 23, 24, 25, 26, -1, 27, 28, // 80
      29, 30, 31, -1, -1, -1, -1, -1, // 88
      -1, 10, 11, 12, 13, 14, 15, 16, // 96
      17, 1, 18, 19, 1, 20, 21, 0,    // 104
      22, 23, 24, 25, 26, -1, 27, 28, // 112
      29, 30, 31                      // 120
    )

    val ENCODING_LENGTH = 32

    val TIMESTAMP_LENGTH = 10
    val RANDOM_LENGTH    = 16
    val ULID_LENGTH: Int = TIMESTAMP_LENGTH + RANDOM_LENGTH

    val MIN_TIME = 0x0L
    val MAX_TIME = 0x0000ffffffffffffL

  }
}

/**
  * ULID generator
  * @param timeSource a function returns the current time in milliseconds (e.g. java.lang.System.currentTimeMillis())
  * @param random a function returns a random value (e.g. scala.util.Random.nextDouble())
  */
private[control] class ULIDGenerator(timeSource: () => Long, random: () => Double) {
  import ULID.constants._

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
          run((time - mod) / ENCODING_LENGTH, ENCODING_CHARS(mod) + out, count + 1)
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
          run(ENCODING_CHARS(index) + out, count + 1)
      }
    }
    run()
  }
}
