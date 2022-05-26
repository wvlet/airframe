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

case class HttpMultiMapEntry(key: String, value: String) {
  override def toString: String = s"${key}:${value}"
}

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
  * Immutable case-insensitive MultiMap structure for representing Http headers, query parameters, etc.
  */
case class HttpMultiMap(private val underlying: Map[String, Any] = Map.empty) {

  private[http] def getUnderlyingMap: Map[String, Any] = underlying

  override def toString: String = {
    entries.mkString(",")
  }

  def toMultiMap: Map[String, Seq[String]] = {
    val m = for ((k, v) <- underlying) yield {
      v match {
        case s: Seq[String @unchecked] =>
          k -> s
        case other =>
          k -> Seq(other.toString)
      }
    }
    m.toMap
  }

  def toSeq: Seq[HttpMultiMapEntry] = entries

  def entries: Seq[HttpMultiMapEntry] = {
    val b = Seq.newBuilder[HttpMultiMapEntry]
    for ((k, v) <- underlying) {
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
      underlying = underlying + (findKey(key) -> value)
    )
  }

  /**
    * @param key
    * @param value
    * @return
    */
  def add(key: String, value: String): HttpMultiMap = {
    val newMap =
      underlying
        .find(_._1.equalsIgnoreCase(key))
        .map { entry =>
          val newValue = entry._2 match {
            case s: String                   => Seq(s, value)
            case lst: Seq[String @unchecked] => lst +: value
          }
          // Use the already existing key for avoid case-insensitive key duplication
          underlying + (entry._1 -> newValue)
        }.getOrElse {
          underlying + (key -> value)
        }
    this.copy(underlying = newMap)
  }

  def +(p: (String, String)): HttpMultiMap = {
    this.add(p._1, p._2)
  }

  def ++(m: Map[String, String]): HttpMultiMap = {
    m.foldLeft(this) { case (mm, entry) =>
      mm.add(entry._1, entry._2)
    }
  }

  def remove(key: String): HttpMultiMap = {
    this.copy(underlying = underlying.filterNot(_._1.equalsIgnoreCase(key)))
  }

  private def findKey(key: String): String = {
    underlying.find(_._1.equalsIgnoreCase(key)).map(_._1).getOrElse(key)
  }

  private def caseInsensitiveSearch(key: String): Option[Any] = {
    // NOTE: Using case insensitive search is O(N) for every insert and look-up, but practically it's not so bad as
    // the number of HTTP headers is not so large.
    underlying.find(_._1.equalsIgnoreCase(key)).map(_._2)
  }

  def get(key: String): Option[String] = {
    caseInsensitiveSearch(key).flatMap { (v: Any) =>
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
    caseInsensitiveSearch(key)
      .map { (v: Any) =>
        v match {
          case s: String                   => Seq(s)
          case lst: Seq[String @unchecked] => lst
          case null                        => Seq.empty
          case v                           => Seq(v.toString)
        }
      }.getOrElse(Seq.empty)
  }

  def isEmpty: Boolean  = underlying.isEmpty
  def nonEmpty: Boolean = !isEmpty

}
