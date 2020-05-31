package wvlet.airframe.metrics

import java.util.regex.Pattern

import scala.annotation.tailrec
import scala.concurrent.duration._

/**
  * Scala version of io.airlift.unit.Duration
  */
case class ElapsedTime(value: Double, unit: TimeUnit) extends Comparable[ElapsedTime] {
  import ElapsedTime._

  require(!value.isInfinite, s"infinite size")
  require(!value.isNaN, s"value is not a number")
  require(value >= 0, s"negative size ${value}, ${unit}")

  def toMillis: Double = roundTo(MILLISECONDS)

  def valueIn(targetUnit: TimeUnit): Double = {
    value * millisPerTimeUnit(unit) * 1.0 / millisPerTimeUnit(targetUnit)
  }

  def roundTo(targetUnit: TimeUnit): Double        = Math.floor(valueIn(targetUnit) + 0.5d)
  def convertTo(targetUnit: TimeUnit): ElapsedTime = ElapsedTime(valueIn(targetUnit), targetUnit)

  def convertToMostSuccinctTimeUnit: ElapsedTime = {
    @tailrec
    def unitToUse(current: TimeUnit, unitsToTest: List[TimeUnit]): TimeUnit = {
      if (unitsToTest.isEmpty)
        current
      else {
        val next = unitsToTest.head
        if (valueIn(next) > 0.9999)
          unitToUse(next, unitsToTest.tail)
        else
          current
      }
    }

    convertTo(unitToUse(NANOSECONDS, units))
  }

  override def toString: String = {
    val magnitude            = valueIn(unit)
    val timeUnitAbbreviation = timeUnitToString(unit)
    return f"${magnitude}%.2f${timeUnitAbbreviation}"
  }
  override def compareTo(o: ElapsedTime) = {
    valueIn(MILLISECONDS).compareTo(o.valueIn(MILLISECONDS))
  }
}

object ElapsedTime {
  def units = List(NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS)

  private val PATTERN = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]+)\\s*$")

  def nanosSince(start: Long): ElapsedTime    = succinctNanos(System.nanoTime - start)
  def succinctNanos(nanos: Long): ElapsedTime = succinctDuration(nanos, NANOSECONDS)
  def succinctDuration(value: Double, unit: TimeUnit): ElapsedTime =
    ElapsedTime(value, unit).convertToMostSuccinctTimeUnit
  def succinctMillis(milliSeconds: Long): ElapsedTime =
    ElapsedTime(milliSeconds, MILLISECONDS).convertToMostSuccinctTimeUnit
  def succinctMillis(milliSeconds: Double): ElapsedTime =
    ElapsedTime(milliSeconds.round, MILLISECONDS).convertToMostSuccinctTimeUnit

  def apply(elapsedTimeStr: String): ElapsedTime = parse(elapsedTimeStr)

  def parse(s: String): ElapsedTime = {
    val m = PATTERN.matcher(s)
    if (!m.matches()) {
      throw new IllegalArgumentException(s"${s} is not a valid duration string")
    }
    val value      = m.group(1).toDouble
    val unitString = m.group(2)
    ElapsedTime(value, valueOfTimeUnit(unitString))
  }

  def millisPerTimeUnit(timeUnit: TimeUnit): Double =
    timeUnit match {
      case NANOSECONDS =>
        1.0 / 1000000.0
      case MICROSECONDS =>
        1.0 / 1000.0
      case MILLISECONDS =>
        1
      case SECONDS =>
        1000
      case MINUTES =>
        1000 * 60
      case HOURS =>
        1000 * 60 * 60
      case DAYS =>
        1000 * 60 * 60 * 24
      case _ =>
        throw new IllegalArgumentException("Unsupported time unit " + timeUnit)
    }

  def timeUnitToString(timeUnit: TimeUnit): String = {
    timeUnit match {
      case NANOSECONDS =>
        "ns"
      case MICROSECONDS =>
        "us"
      case MILLISECONDS =>
        "ms"
      case SECONDS =>
        "s"
      case MINUTES =>
        "m"
      case HOURS =>
        "h"
      case DAYS =>
        "d"
      case _ =>
        throw new IllegalArgumentException("Unsupported time unit " + timeUnit)
    }
  }

  def valueOfTimeUnit(timeUnitString: String): TimeUnit = {
    timeUnitString match {
      case "ns" =>
        NANOSECONDS
      case "us" =>
        MICROSECONDS
      case "ms" =>
        MILLISECONDS
      case "s" =>
        SECONDS
      case "m" =>
        MINUTES
      case "h" =>
        HOURS
      case "d" =>
        DAYS
      case _ =>
        throw new IllegalArgumentException("Unknown time unit: " + timeUnitString)
    }
  }
}
