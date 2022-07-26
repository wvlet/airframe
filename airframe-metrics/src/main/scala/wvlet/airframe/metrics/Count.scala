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
package wvlet.airframe.metrics
import wvlet.airframe.metrics.Count.CountUnit
import wvlet.airframe.surface.{Surface, Zero}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/**
  * A human-readable integer (non-decimal number) representation.
  *
  * @param value
  * @param unit
  */
case class Count(value: Long, unit: CountUnit) extends Comparable[Count] {
  override def toString: String = {
    if (unit == Count.ONE) {
      f"${value}%,d"
    } else if (valueOf(unit) == (value / unit.factor)) {
      f"${valueOf(unit).toLong}%,d${unit.unitString}"
    } else {
      f"${valueOf(unit)}%.2f${unit.unitString}"
    }
  }

  def toLong: Long = value
  def valueOf(unit: CountUnit): Double = {
    value * 1.0 / unit.factor
  }

  def convertTo(unit: CountUnit): Count = {
    Count(value, unit)
  }

  def mostSuccinctCount: Count = {
    @tailrec
    def loop(unit: CountUnit, remaining: List[CountUnit]): CountUnit = {
      if (remaining.isEmpty) {
        unit
      } else {
        val nextUnit = remaining.head
        if (math.abs(valueOf(nextUnit)) < 1.0) {
          unit
        } else {
          loop(nextUnit, remaining.tail)
        }
      }
    }
    val targetUnit = loop(Count.ONE, Count.units.tail)
    convertTo(targetUnit)
  }
  override def compareTo(o: Count): Int = {
    value.compareTo(o.value)
  }
}

/**
  */
object Count {
  Zero.register(Surface.of[CountUnit], ONE)
  Zero.register(Surface.of[Count], Count(0))

  val units             = List(ONE, THOUSAND, MILLION, BILLION, TRILLION, QUADRILLION)
  private val unitTable = units.map(x => x.unitString -> x).toMap[String, CountUnit]

  sealed abstract class CountUnit(val factor: Long, val unitString: String) {
    override def toString = unitString
  }
  case object ONE         extends CountUnit(1L, "")
  case object THOUSAND    extends CountUnit(1000L, "K")
  case object MILLION     extends CountUnit(1000000L, "M")
  case object BILLION     extends CountUnit(1000000000L, "B")
  case object TRILLION    extends CountUnit(1000000000000L, "T")
  case object QUADRILLION extends CountUnit(1000000000000000L, "Q")

  object CountUnit {
    // deserializer for airframe-codec
    def unapply(unitString: String): Option[CountUnit] = units.find(_.unitString == unitString)
  }

  def succinct(x: Long): Count = {
    Count(x, ONE).mostSuccinctCount
  }

  def apply(value: Long): Count                = Count(value, ONE)
  def unapply(countStr: String): Option[Count] = Try(apply(countStr)).toOption

  private val countPattern = """^\s*(?<num>(?:-?)?[\d,]+(?:\.\d+)?)\s*(?<unit>[a-zA-Z])\s*$""".r
  def apply(countStr: String): Count = {
    countPattern.findFirstMatchIn(countStr) match {
      case None =>
        // When no unit string is found
        val normalized = countStr.replaceAll(",", "")
        Try(normalized.toLong) match {
          case Success(v) => Count(v)
          case Failure(e) =>
            // Try parsing as double
            Try(normalized.toDouble) match {
              case Success(d) => Count(d.toLong)
              case Failure(e) =>
                throw new IllegalArgumentException(s"Invalid count string: ${countStr}")
            }
        }
      case Some(m) =>
        val num  = m.group("num").replaceAll(",", "").toDouble
        val unit = m.group("unit")
        unitTable.get(unit) match {
          case None =>
            throw new IllegalArgumentException(s"Invalid count unit ${unit} in ${countStr}")
          case Some(u) =>
            Count((num * u.factor).round, u)
        }
    }
  }

}
