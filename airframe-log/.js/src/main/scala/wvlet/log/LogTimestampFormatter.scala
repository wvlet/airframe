package wvlet.log

import scala.scalajs.js

/**
  * Use scalajs.js.Date to foramte timestamp
  */
object LogTimestampFormatter {
  def formatTimestamp(timeMillis: Long): String = {
    new js.Date(timeMillis).toISOString()
  }

  def formatTimestampWithNoSpaace(timeMillis: Long): String = {
    new js.Date(timeMillis).toISOString()
  }
}
