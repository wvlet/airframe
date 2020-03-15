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

case class HttpMultiMapEntry(key: String, value: String)

object HttpMultiMap {
  val empty: HttpMultiMap = HttpMultiMap()

  def newBuilder: HttpMultiMapBuilder = new HttpMultiMapBuilder()

  class HttpMultiMapBuilder(private var m: HttpMultiMap = HttpMultiMap.empty) {
    def add(k: String, v: String): HttpMultiMapBuilder = {
      m = m.add(k, v)
      this
    }
    def +=(e: (String, String)): HttpMultiMapBuilder = {
      add(e._1, e._2)
    }
    def ++=(entries: Seq[(String, String)]): HttpMultiMapBuilder = {
      entries.foldLeft(this)((m, entry) => m.add(entry._1, entry._2))
    }
    def result(): HttpMultiMap = m
  }
}

/**
  * Immutable MultiMap structure for representing Http headers, query parameters, etc.
  */
case class HttpMultiMap(private[http] val map: Map[String, Any] = Map.empty) {

  override def toString: String = {
    toSeq.mkString(",")
  }

  def toMultiMap: Map[String, Seq[String]] = {
    val m = for ((k, v) <- map) yield {
      v match {
        case s: Seq[String @unchecked] =>
          k -> s
        case other =>
          k -> Seq(other.toString)
      }
    }
    m.toMap
  }

  def entries: Seq[(String, String)] = {
    toSeq.map(x => x.key -> x.value)
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

  def get(key: String): Option[String] = {
    map.get(key).flatMap { v: Any =>
      v match {
        case s: String                   => Some(s)
        case lst: Seq[String @unchecked] => Option(lst.head)
        case null                        => None
        case _                           => Some(v.toString)
      }
    }
  }

  def getOrElse(key: String, defaultValue: => String): String = {
    get(key).getOrElse(defaultValue)
  }

  def getAll(key: String): Seq[String] = {
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

  def isEmpty: Boolean = map.isEmpty

}
