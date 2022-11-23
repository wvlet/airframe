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

object PrefixedULID {
  val DELIMITER = ':'

  def newPrefixedULID(prefix: String): PrefixedULID = {
    PrefixedULID(prefix, ULID.newULID)
  }
  def newPrefixedULIDString(prefix: String): String = {
    s"${prefix}:${ULID.newULIDString}"
  }

  /**
    * Parse the given PrefixedULID string
    */
  def fromString(s: String): PrefixedULID = {
    val pos = s.lastIndexOf(PrefixedULID.DELIMITER)
    if (pos == -1) {
      throw new IllegalArgumentException(
        s"No delimiter ${PrefixedULID.DELIMITER} is found before the ULID string: ${s}"
      )
    }
    val prefix = s.substring(0, pos)
    val ulid   = ULID.fromString(s.substring(pos + 1))
    PrefixedULID(prefix, ulid)
  }
}

case class PrefixedULID(prefix: String, ulid: ULID) extends Ordered[PrefixedULID] {
  override def toString: String = s"${prefix}${PrefixedULID.DELIMITER}${ulid}"

  override def compare(that: PrefixedULID): Int = {
    this.prefix.compare(that.prefix) match {
      case 0 =>
        this.ulid.compare(that.ulid)
      case cmp =>
        cmp
    }
  }
}
