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

import scalanative.posix.time.*
import scalanative.unsafe.*
import scalanative.unsigned.*
import scalanative.libc.stdio.*
import scalanative.libc.string.*

/**
  * Use strftime to format timestamps in Scala Native
  */
object LogTimestampFormatter {

  private def format(pattern: CString, timeMillis: Long): String = {
    Zone {
      val ttPtr = alloc[time_t]()
      !ttPtr = (timeMillis / 1000).toSize
      val tmPtr = alloc[tm]()
      localtime_r(ttPtr, tmPtr)
      val bufSize        = 29.toUSize // max size for time strings
      val buf: Ptr[Byte] = alloc[Byte](bufSize)
      strftime(buf, bufSize, pattern, tmPtr)
      val ms = timeMillis % 1000

      val msBuf: Ptr[Byte] = alloc[Byte](3)
      sprintf(msBuf, c"%03d", ms)
      strcat(buf, msBuf)

      val tzBuf: Ptr[Byte] = alloc[Byte](5)
      strftime(tzBuf, 5.toUSize, c"%z", tmPtr)
      if strlen(tzBuf) <= 1.toUSize then {
        // For UTC-00:00
        strcat(buf, c"Z")
      } else {
        strcat(buf, tzBuf)
      }
      fromCString(buf)
    }
  }

  def formatTimestamp(timeMillis: Long): String =
    format(c"%Y-%m-%d %H:%M:%S.", timeMillis)

  def formatTimestampWithNoSpaace(timeMillis: Long): String =
    format(c"%Y-%m-%dT%H:%M:%S.", timeMillis)
}
