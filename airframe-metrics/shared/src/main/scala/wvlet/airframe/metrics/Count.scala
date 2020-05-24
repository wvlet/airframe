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

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/**
  * A human-readable number representation.
  *
  * @param value
  * @param unit
  */
case class Count(value: Double, unit: CountUnit) extends Comparable[Count] {
  require(!value.isInfinity, s"Infinite count")
  require(!value.isNaN, s"size is not a number")
  require(value >= 0, s"negative count: ${value}, ${unit}")

  override def toString: String = {
    if (unit == Count.ONE) {
      f"${value.toLong}%,d"
    } else if (value.floor == value) {
      s"${value.floor.toLong}${unit.unitString}"
    } else {
      f"${value}%.2f${unit.unitString}"
    }
  }

  def toLong: Long = {
    (value * unit.factor).toLong
  }

  def valueOf(unit: CountUnit): Double = {
    value * (this.unit.factor * 1.0 / unit.factor)
  }

  def covnertTo(unit: CountUnit): Count = {
    Count(valueOf(unit), unit)
  }

  def mostSuccinctCount: Count = {
    @tailrec
    def loop(unit: CountUnit, remaining: List[CountUnit]): CountUnit = {
      if (remaining.isEmpty) {
        unit
      } else {
        val nextUnit = remaining.head
        if (valueOf(nextUnit) < 1.0) {
          unit
        } else {
          loop(nextUnit, remaining.tail)
        }
      }
    }
    val targetUnit = loop(Count.ONE, Count.units.tail)
    covnertTo(targetUnit)
  }
  override def compareTo(o: Count): Int = {
    valueOf(Count.ONE).compareTo(o.valueOf(Count.ONE))
  }
}

/**
  *
  */
object Count {

  sealed class CountUnit private[metrics] (val factor: Long, val unitString: String)

  case object ONE         extends CountUnit(1, "")
  case object THOUSAND    extends CountUnit(1000L, "K")
  case object MILLION     extends CountUnit(1000000L, "M")
  case object BILLION     extends CountUnit(1000000000L, "B")
  case object TRILLION    extends CountUnit(1000000000000L, "T")
  case object QUADRILLION extends CountUnit(1000000000000000L, "Q")

  val units             = List(ONE, THOUSAND, MILLION, BILLION, TRILLION, QUADRILLION)
  private val unitTable = units.map(x => x.unitString -> x).toMap[String, CountUnit]

  def succinct(x: Long): Count = {
    Count(x, ONE).mostSuccinctCount
  }
  def succinct(x: Double): Count = {
    Count(x, ONE).mostSuccinctCount
  }
  def apply(value: Long): Count   = Count(value, ONE)
  def apply(value: Double): Count = Count(value, ONE)

  def unapply(countStr: String): Option[Count] = Try(apply(countStr)).toOption

  private val countPattern = """^\s*(\d+(?:\.\d+)?)\s*([a-zA-Z])\s*$""".r("num", "unit")
  def apply(countStr: String): Count = {
    countPattern.findFirstMatchIn(countStr) match {
      case None =>
        Try(countStr.toLong) match {
          case Success(v) => Count(v)
          case Failure(e) =>
            throw new IllegalArgumentException(s"Invalid count string: ${countStr}")
        }
      case Some(m) =>
        val num  = m.group("num").toDouble
        val unit = m.group("unit")
        unitTable.get(unit) match {
          case None =>
            throw new IllegalArgumentException(s"Invalid count unit ${unit} in ${countStr}")
          case Some(u) =>
            Count(num, u)
        }
    }
  }

}
