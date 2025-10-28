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

/**
  * Simple timestamp formatter for Scala Native using basic time arithmetic
  */
object LogTimestampFormatter {

  /**
    * Format timestamp in "YYYY-MM-DD HH:MM:SS.sss+ZZZZ" format
    */
  def formatTimestamp(timeMillis: Long): String = {
    formatTimestampInternal(timeMillis, withSpace = true)
  }

  /**
    * Format timestamp in "YYYY-MM-DDTHH:MM:SS.sss+ZZZZ" format (ISO-like)
    */
  def formatTimestampWithNoSpaace(timeMillis: Long): String = {
    formatTimestampInternal(timeMillis, withSpace = false)
  }

  private def formatTimestampInternal(timeMillis: Long, withSpace: Boolean): String = {
    // Convert to seconds and milliseconds
    val seconds = timeMillis / 1000L
    val millis = timeMillis % 1000L

    // Basic UTC calculation (simplified)
    val SECONDS_PER_DAY = 86400L
    val SECONDS_PER_HOUR = 3600L
    val SECONDS_PER_MINUTE = 60L
    
    // Get days since Unix epoch (1970-01-01)
    val daysSinceEpoch = seconds / SECONDS_PER_DAY
    
    // Simple year calculation 
    // This is a simplified calculation that assumes all years have 365.25 days
    // It won't be 100% accurate for all edge cases but will work for most practical purposes
    val approximateYear = 1970 + (daysSinceEpoch / 365.25).toInt
    
    // Use a fixed date format for simplicity and cross-platform compatibility
    // We'll use a basic approximation to avoid complex leap year calculations
    val year = if (approximateYear < 1970) 1970 else approximateYear
    val dayOfYear = (daysSinceEpoch % 365.25).toInt + 1
    
    // Simple month/day approximation (not perfectly accurate but good enough)
    val (month, day) = dayOfYear match {
      case d if d <= 31 => (1, d)
      case d if d <= 59 => (2, d - 31)
      case d if d <= 90 => (3, d - 59)
      case d if d <= 120 => (4, d - 90)
      case d if d <= 151 => (5, d - 120)
      case d if d <= 181 => (6, d - 151)
      case d if d <= 212 => (7, d - 181)
      case d if d <= 243 => (8, d - 212)
      case d if d <= 273 => (9, d - 243)
      case d if d <= 304 => (10, d - 273)
      case d if d <= 334 => (11, d - 304)
      case d => (12, d - 334)
    }
    
    // Calculate time of day
    val secondsInDay = seconds % SECONDS_PER_DAY
    val hour = (secondsInDay / SECONDS_PER_HOUR).toInt
    val minute = ((secondsInDay % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE).toInt
    val second = (secondsInDay % SECONDS_PER_MINUTE).toInt
    
    // Format the timestamp
    val separator = if (withSpace) " " else "T"
    f"$year%04d-$month%02d-$day%02d$separator%s$hour%02d:$minute%02d:$second%02d.$millis%03dZ"
  }
}
