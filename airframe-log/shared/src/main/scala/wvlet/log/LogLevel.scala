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
package wvlet.log

import java.util.logging.Level

/**
  * log level definitions
  */
object LogLevel {
  case object OFF   extends LogLevel(0, Level.OFF, "off")
  case object ERROR extends LogLevel(1, Level.SEVERE, "error")
  case object WARN  extends LogLevel(2, Level.WARNING, "warn")
  case object INFO  extends LogLevel(3, Level.INFO, "info")
  case object DEBUG extends LogLevel(4, Level.FINE, "debug")
  case object TRACE extends LogLevel(5, Level.FINER, "trace")
  case object ALL   extends LogLevel(6, Level.ALL, "all")

  val values                    = IndexedSeq(OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL)
  private lazy val index        = values.map { l => l.name.toLowerCase -> l }.toMap
  private lazy val jlLevelIndex = values.map { l => l.jlLevel -> l }.toMap

  def apply(name: String): LogLevel = {
    val n  = name.toLowerCase()
    val lv = values.find(n == _.name)
    if (lv.isEmpty) {
      Console.err.println(s"Unknown log level [${name}] Use info log level instead.")
      INFO
    } else
      lv.get
  }

  def apply(jlLevel: Level): LogLevel = {
    jlLevelIndex.get(jlLevel) match {
      case Some(l) => l
      case None =>
        jlLevel match {
          case Level.CONFIG => INFO
          case Level.FINEST => TRACE
        }
    }
  }

  def unapply(name: String): Option[LogLevel] = index.get(name.toLowerCase)

  implicit object LogOrdering extends Ordering[LogLevel] {
    override def compare(x: LogLevel, y: LogLevel): Int = x.order - y.order
  }
}

sealed abstract class LogLevel(val order: Int, val jlLevel: Level, val name: String)
    extends Ordered[LogLevel]
    with Serializable {
  def compare(other: LogLevel) = this.order - other.order
}
