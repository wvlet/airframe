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
      val bufSize = 26.toUSize
      val buf: Ptr[Byte] = alloc[Byte](bufSize)
      strftime(buf, bufSize, pattern, tmPtr)
      val ms = timeMillis % 1000

      val msBuf: Ptr[Byte] = alloc[Byte](3)
      sprintf(msBuf, c"%03d", ms)
      strcat(buf, msBuf)

      val tzBuf: Ptr[Byte] = alloc[Byte](5)
      strftime(tzBuf, 5.toUSize, c"%z", tmPtr)
      strcat(buf, tzBuf)
      fromCString(buf)
    }
  }

  def formatTimestamp(timeMillis: Long): String = {
    format(c"%Y-%m-%d %H:%M:%S.", timeMillis)
  }

  def formatTimestampWithNoSpaace(timeMillis: Long): String = {
    format(c"%Y-%m-%dT%H:%M:%S.", timeMillis)
  }
}
