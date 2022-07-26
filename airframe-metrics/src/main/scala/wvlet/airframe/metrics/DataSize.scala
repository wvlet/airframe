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

import wvlet.airframe.metrics.DataSize._
import wvlet.airframe.surface.{Surface, Zero}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/**
  * A human-readable data size representation. This is a re-implementation of
  * https://github.com/airlift/units/blob/master/src/main/java/io/airlift/units/DataSize.java for Scala.
  */
case class DataSize(value: Double, unit: DataSizeUnit) extends Comparable[DataSize] {
  require(!value.isInfinite, s"infinite size")
  require(!value.isNaN, s"size is not a number")
  require(value >= 0, s"negative size ${value}, ${unit}")

  override def toString: String = {
    // Has fraction?
    if (value.floor == value) {
      s"${value.floor.toLong}${unit.unitString}"
    } else {
      f"${value}%.2f${unit.unitString}"
    }
  }

  def toBytes: Long = {
    val bytes = valueOf(BYTE)
    checkState(bytes <= Long.MaxValue, s"${this} is too large to represent in bytes in Long type")
    bytes.toLong
  }

  def valueOf(unit: DataSizeUnit): Double = {
    value * (this.unit.factor * 1.0 / unit.factor)
  }

  def convertTo(unit: DataSizeUnit): DataSize = DataSize(valueOf(unit), unit)

  def mostSuccinctDataSize: DataSize = {
    @tailrec
    def loop(unit: DataSizeUnit, remaining: List[DataSizeUnit]): DataSizeUnit = {
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
    val targetUnit = loop(BYTE, units.tail)
    convertTo(targetUnit)
  }

  def roundTo(unit: DataSizeUnit): Long = {
    val rounded = (valueOf(unit) + 0.5d).floor
    checkState(rounded <= Long.MaxValue, s"${this} is too large to represent in ${unit.unitString} in Long type")
    rounded.toLong
  }

  override def compareTo(o: DataSize) = {
    valueOf(BYTE).compareTo(o.valueOf(BYTE))
  }
}

object DataSize {
  Zero.register(Surface.of[DataSizeUnit], BYTE)
  Zero.register(Surface.of[DataSize], DataSize(0, BYTE))

  private[metrics] def checkState(preCondition: Boolean, errorMessage: String): Unit = {
    if (!preCondition) {
      throw new IllegalStateException(errorMessage)
    }
  }

  implicit class StrToDataSizeConverter(dataSizeStr: String) {
    def toDataSize = DataSize.apply(dataSizeStr)
  }
  implicit class LongToDataSizeConverter(bytes: Long) {
    def toDataSize = DataSize.apply(bytes)
  }

  private val dataSizePattern = """^\s*(?<num>\d+(?:\.\d+)?)\s*(?<unit>[a-zA-Z]+)\s*$""".r

  sealed abstract class DataSizeUnit(val factor: Long, val unitString: String)
  case object BYTE     extends DataSizeUnit(1L, "B")
  case object KILOBYTE extends DataSizeUnit(1L << 10, "kB")
  case object MEGABYTE extends DataSizeUnit(1L << 20, "MB")
  case object GIGABYTE extends DataSizeUnit(1L << 30, "GB")
  case object TERABYTE extends DataSizeUnit(1L << 40, "TB")
  case object PETABYTE extends DataSizeUnit(1L << 50, "PB")

  val units             = List(BYTE, KILOBYTE, MEGABYTE, GIGABYTE, TERABYTE, PETABYTE)
  private val unitTable = units.map(x => x.unitString -> x).toMap[String, DataSizeUnit]

  def succinct(bytes: Long): DataSize = DataSize(bytes.toDouble, BYTE).mostSuccinctDataSize

  def apply(bytes: Long): DataSize = DataSize(bytes.toDouble, BYTE)

  def apply(dataSizeStr: String): DataSize = {
    dataSizePattern.findFirstMatchIn(dataSizeStr) match {
      case None =>
        Try(dataSizeStr.toLong) match {
          case Success(l) =>
            DataSize(l)
          case Failure(e) =>
            throw new IllegalArgumentException(s"Invalid data size string: ${dataSizeStr}")
        }
      case Some(m) =>
        val num  = m.group("num").toDouble
        val unit = m.group("unit")
        unitTable.get(unit) match {
          case None => throw new IllegalArgumentException(s"Invalid data unit ${unit} in ${dataSizeStr}")
          case Some(u) =>
            DataSize(num, u)
        }
    }
  }
}

object DataSizeUnit {}
