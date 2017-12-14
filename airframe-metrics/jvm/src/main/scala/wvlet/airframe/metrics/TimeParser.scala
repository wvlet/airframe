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

import java.time._
import java.time.format.DateTimeFormatter

import wvlet.log.LogSupport

import scala.util.{Failure, Success, Try}

/**
  * Time string to ZonedDateTime converter
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

  def parseAtLocalTimeZone(s: String) = parse(s, systemTimeZone)

  def parse(s: String, zone: ZoneOffset): Option[ZonedDateTime] = {
    parseLocalDateTime(s, zone)
      .orElse {
        parseZonedDateTime(s)
      }
  }

}
