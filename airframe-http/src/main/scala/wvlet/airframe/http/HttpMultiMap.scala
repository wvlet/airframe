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
package wvlet.airframe.http

/**
  * Read-only immutable MultiMap interface. Use getAll to get all values for a key.
  */
trait HttpMultiMapAccess {
  def get(key: String): Option[String]
  def getAll(key: String): Seq[String]
  def isEmpty: Boolean
}

case class HttpMultiMapEntry(key: String, value: String)

object HttpMultiMap {
  val empty: HttpMultiMap = HttpMultiMap()
}

/**
  * Immutable MultiMap structure for representing Http headers, query parameters, etc.
  */
case class HttpMultiMap(private[http] val map: Map[String, Any] = Map.empty) extends HttpMultiMapAccess {

  override def toString: String = {
    toSeq.mkString(",")
  }

  def toSeq: Seq[HttpMultiMapEntry] = {
    val b = Seq.newBuilder[HttpMultiMapEntry]
    for ((k, v) <- map) {
      v match {
        case s: String =>
          b += HttpMultiMapEntry(k, s)
        case lst: Seq[String @unchecked] =>
          b ++= lst.map { x => HttpMultiMapEntry(k, x) }
        case null =>
        // do nothing
        case other =>
          b += HttpMultiMapEntry(k, other.toString)
      }
    }
    b.result()
  }

  def set(key: String, value: String): HttpMultiMap = {
    this.copy(
      map = map + (key -> value)
    )
  }

  /**
    *
    * @param key
    * @param value
    * @return
    */
  def add(key: String, value: String): HttpMultiMap = {
    val newMap = map
      .get(key).map { v =>
        val newValue = v match {
          case s: String                   => Seq(s, value)
          case lst: Seq[String @unchecked] => lst +: value
        }
        map + (key -> newValue)
      }.getOrElse {
        map + (key -> value)
      }

    this.copy(map = newMap)
  }

  def ++(m: Map[String, String]): HttpMultiMap = {
    m.foldLeft(this) {
      case (mm, entry) =>
        mm.add(entry._1, entry._2)
    }
  }

  def remove(key: String): HttpMultiMap = {
    this.copy(map = map - key)
  }

  override def get(key: String): Option[String] = {
    map.get(key).flatMap { v: Any =>
      v match {
        case s: String                   => Some(s)
        case lst: Seq[String @unchecked] => Option(lst.head)
        case null                        => None
        case _                           => Some(v.toString)
      }
    }
  }

  override def getAll(key: String): Seq[String] = {
    map
      .get(key).map { v: Any =>
        v match {
          case s: String                   => Seq(s)
          case lst: Seq[String @unchecked] => lst
          case null                        => Seq.empty
          case v                           => Seq(v.toString)
        }
      }.getOrElse(Seq.empty)
  }

  override def isEmpty: Boolean = map.isEmpty

}
