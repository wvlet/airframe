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

object HttpHeader {
  val empty = HttpHeader(Map.empty)

  case class HeaderEntry(key: String, value: String)
}

import HttpHeader._

/**
  * An immutable multi-map implementation for storing HTTP headers
  */
case class HttpHeader(private val map: Map[String, Any]) {
  def set(key: String, value: String): HttpHeader = {
    this.copy(
      map = map + (key -> value)
    )
  }
  def add(key: String, value: String): HttpHeader = {
    val newMap = map
      .get(key).map { v =>
        val newValue = v match {
          case s: String        => Seq(s, value)
          case lst: Seq[String] => lst +: value
        }
        map + (key -> newValue)
      }.getOrElse {
        map + (key -> value)
      }

    this.copy(map = newMap)
  }
  def remove(key: String): HttpHeader = {
    this.copy(map = map - key)
  }

  def get(key: String): Option[String] = {
    map.get(key).flatMap { v: Any =>
      v match {
        case s: String        => Some(s)
        case lst: Seq[String] => Option(lst.head)
        case null             => None
        case _                => Some(v.toString)
      }
    }
  }

  def getAll(key: String): Seq[String] = {
    map
      .get(key).map { v: Any =>
        v match {
          case s: String        => Seq(s)
          case lst: Seq[String] => lst
          case null             => Seq.empty
          case v                => Seq(v.toString)
        }
      }.getOrElse(Seq.empty)
  }

  def isEmpty: Boolean = map.isEmpty

  def toSeq: Seq[HeaderEntry] = {
    val b = Seq.newBuilder[HeaderEntry]
    for ((k, v) <- map) {
      v match {
        case s: String =>
          b += HeaderEntry(k, s)
        case lst: Seq[String] =>
          b ++= lst.map { x => HeaderEntry(k, x) }
        case null =>
        // do nothing
        case other =>
          b += HeaderEntry(k, other.toString)
      }
    }
    b.result()
  }
}
