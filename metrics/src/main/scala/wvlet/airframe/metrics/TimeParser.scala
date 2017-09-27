package wvlet.airframe.metrics

import java.time.format.DateTimeFormatter
import java.time._
import java.time.temporal.{TemporalAccessor, TemporalQueries, TemporalQuery}

import wvlet.log.LogSupport

import scala.util.{Failure, Success, Try}

/**
  *
  */
object TimeParser extends LogSupport {

  val localDatePattern     = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val localDateTimePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]")

  val zonedDateTimePatterns: List[DateTimeFormatter] = List(
    "yyyy-MM-dd HH:mm:ss[.SSS][ z][XXXXX][XXXX]['['VV']']",
    "yyyy-MM-dd'T'HH:mm:ss[.SSS][ z][XXXXX][XXXX]['['VV']']"
  ).map(DateTimeFormatter.ofPattern(_))

  def parseLocalDateTime(s: String, zone: ZoneOffset): Option[ZonedDateTime] = {
    Try(LocalDateTime.parse(s, localDateTimePattern))
      .map { d =>
        ZonedDateTime.of(d, zone)
      }
      .orElse {
        Try(LocalDate.parse(s, localDatePattern))
          .map { d =>
            d.atStartOfDay(zone)
          }
      }
      .toOption
  }

  def parseZonedDateTime(s: String): Option[ZonedDateTime] = {
    def loop(lst: List[DateTimeFormatter]): Option[ZonedDateTime] = {
      if (lst.isEmpty)
        None
      else {
        val formatter = lst.head
        Try(ZonedDateTime.parse(s, formatter)) match {
          case Success(dt) =>
            Some(dt)
          case Failure(e) =>
            loop(lst.tail)
        }
      }
    }
    loop(zonedDateTimePatterns.toList)
  }

  def parseAtLocalTimeZone(s: String) = parse(s, TimeWindow.systemZone)

  def parse(s: String, zone: ZoneOffset): Option[ZonedDateTime] = {
    parseLocalDateTime(s, zone)
      .orElse {
        parseZonedDateTime(s)
      }
  }
}
