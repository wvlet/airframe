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
package wvlet.airframe.http.client

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import scala.scalanative.libc.stdlib
import scala.scalanative.libc.string

/**
  * Scala Native bindings for libcurl.
  *
  * This provides low-level access to libcurl functions for making HTTP requests. Requires libcurl 7.56.0 or newer to be
  * installed on the system.
  *
  * @see
  *   https://curl.se/libcurl/c/
  */
@link("curl")
@extern
object CurlBindings {

  /**
    * Opaque handle for a curl easy session
    */
  type CURL = Ptr[Byte]

  /**
    * Linked list structure for headers and other string lists
    */
  type curl_slist = CStruct2[CString, Ptr[Byte]]

  // Return codes
  final val CURLE_OK: CInt                       = 0
  final val CURLE_UNSUPPORTED_PROTOCOL: CInt     = 1
  final val CURLE_FAILED_INIT: CInt              = 2
  final val CURLE_URL_MALFORMAT: CInt            = 3
  final val CURLE_COULDNT_RESOLVE_PROXY: CInt    = 5
  final val CURLE_COULDNT_RESOLVE_HOST: CInt     = 6
  final val CURLE_COULDNT_CONNECT: CInt          = 7
  final val CURLE_OPERATION_TIMEDOUT: CInt       = 28
  final val CURLE_SSL_CONNECT_ERROR: CInt        = 35
  final val CURLE_PEER_FAILED_VERIFICATION: CInt = 60

  // Global init flags
  final val CURL_GLOBAL_DEFAULT: CLong = 3L
  final val CURL_GLOBAL_SSL: CLong     = 1L
  final val CURL_GLOBAL_WIN32: CLong   = 2L
  final val CURL_GLOBAL_ALL: CLong     = 3L
  final val CURL_GLOBAL_NOTHING: CLong = 0L

  // CURLOPT options (using actual libcurl values)
  // String options start at 10000
  final val CURLOPT_URL: CInt             = 10002
  final val CURLOPT_PROXY: CInt           = 10004
  final val CURLOPT_USERPWD: CInt         = 10005
  final val CURLOPT_POSTFIELDS: CInt      = 10015
  final val CURLOPT_USERAGENT: CInt       = 10018
  final val CURLOPT_CUSTOMREQUEST: CInt   = 10036
  final val CURLOPT_WRITEDATA: CInt       = 10001
  final val CURLOPT_HEADERDATA: CInt      = 10029
  final val CURLOPT_COPYPOSTFIELDS: CInt  = 10165
  final val CURLOPT_ACCEPT_ENCODING: CInt = 10102

  // Long options start at 0
  final val CURLOPT_PORT: CInt              = 3
  final val CURLOPT_TIMEOUT: CInt           = 13
  final val CURLOPT_CONNECTTIMEOUT: CInt    = 78
  final val CURLOPT_FOLLOWLOCATION: CInt    = 52
  final val CURLOPT_MAXREDIRS: CInt         = 68
  final val CURLOPT_POST: CInt              = 47
  final val CURLOPT_POSTFIELDSIZE: CInt     = 60
  final val CURLOPT_HTTPGET: CInt           = 80
  final val CURLOPT_NOBODY: CInt            = 44
  final val CURLOPT_HEADER: CInt            = 42
  final val CURLOPT_VERBOSE: CInt           = 41
  final val CURLOPT_SSL_VERIFYPEER: CInt    = 64
  final val CURLOPT_SSL_VERIFYHOST: CInt    = 81
  final val CURLOPT_HTTP_VERSION: CInt      = 84
  final val CURLOPT_TIMEOUT_MS: CInt        = 155
  final val CURLOPT_CONNECTTIMEOUT_MS: CInt = 156

  // Function pointer options start at 20000
  final val CURLOPT_WRITEFUNCTION: CInt  = 20011
  final val CURLOPT_HEADERFUNCTION: CInt = 20079

  // Linked list options start at 10000
  final val CURLOPT_HTTPHEADER: CInt = 10023

  // CURLINFO codes for curl_easy_getinfo
  final val CURLINFO_STRING: CInt = 0x100000
  final val CURLINFO_LONG: CInt   = 0x200000
  final val CURLINFO_DOUBLE: CInt = 0x300000

  final val CURLINFO_EFFECTIVE_URL: CInt    = CURLINFO_STRING + 1
  final val CURLINFO_RESPONSE_CODE: CInt    = CURLINFO_LONG + 2
  final val CURLINFO_TOTAL_TIME: CInt       = CURLINFO_DOUBLE + 3
  final val CURLINFO_CONTENT_TYPE: CInt     = CURLINFO_STRING + 18
  final val CURLINFO_CONTENT_LENGTH_DOWNLOAD: CInt = CURLINFO_DOUBLE + 15

  // HTTP version options
  final val CURL_HTTP_VERSION_NONE: CLong = 0L
  final val CURL_HTTP_VERSION_1_0: CLong  = 1L
  final val CURL_HTTP_VERSION_1_1: CLong  = 2L
  final val CURL_HTTP_VERSION_2_0: CLong  = 3L

  /**
    * Global curl initialization. Must be called before any other curl functions.
    */
  def curl_global_init(flags: CLong): CInt = extern

  /**
    * Global curl cleanup. Should be called when done with all curl operations.
    */
  def curl_global_cleanup(): Unit = extern

  /**
    * Create a new curl easy handle.
    */
  def curl_easy_init(): CURL = extern

  /**
    * Clean up and free a curl easy handle.
    */
  def curl_easy_cleanup(curl: CURL): Unit = extern

  /**
    * Reset a curl handle to its initial state.
    */
  def curl_easy_reset(curl: CURL): Unit = extern

  /**
    * Duplicate a curl handle with all its options.
    */
  def curl_easy_duphandle(curl: CURL): CURL = extern

  /**
    * Set options for a curl easy handle (string parameter).
    */
  def curl_easy_setopt(curl: CURL, option: CInt, parameter: CString): CInt = extern

  /**
    * Set options for a curl easy handle (long parameter).
    */
  def curl_easy_setopt(curl: CURL, option: CInt, parameter: CLong): CInt = extern

  /**
    * Set options for a curl easy handle (function pointer parameter).
    */
  def curl_easy_setopt(curl: CURL, option: CInt, parameter: CFuncPtr4[Ptr[Byte], CSize, CSize, Ptr[Byte], CSize]): CInt =
    extern

  /**
    * Set options for a curl easy handle (pointer parameter).
    */
  def curl_easy_setopt(curl: CURL, option: CInt, parameter: Ptr[Byte]): CInt = extern

  /**
    * Set options for a curl easy handle (curl_slist parameter).
    */
  def curl_easy_setopt(curl: CURL, option: CInt, parameter: Ptr[curl_slist]): CInt = extern

  /**
    * Perform the curl request.
    */
  def curl_easy_perform(curl: CURL): CInt = extern

  /**
    * Get information about a completed transfer (long result).
    */
  def curl_easy_getinfo(curl: CURL, info: CInt, result: Ptr[CLong]): CInt = extern

  /**
    * Get information about a completed transfer (string result).
    */
  def curl_easy_getinfo(curl: CURL, info: CInt, result: Ptr[CString]): CInt = extern

  /**
    * Get information about a completed transfer (double result).
    */
  def curl_easy_getinfo(curl: CURL, info: CInt, result: Ptr[CDouble]): CInt = extern

  /**
    * Get a human readable error message for a curl error code.
    */
  def curl_easy_strerror(code: CInt): CString = extern

  /**
    * URL encode a string.
    */
  def curl_easy_escape(curl: CURL, string: CString, length: CInt): CString = extern

  /**
    * URL decode a string.
    */
  def curl_easy_unescape(curl: CURL, string: CString, length: CInt, outlength: Ptr[CInt]): CString = extern

  /**
    * Free memory allocated by curl.
    */
  def curl_free(ptr: Ptr[Byte]): Unit = extern

  /**
    * Append a string to a curl_slist.
    */
  def curl_slist_append(list: Ptr[curl_slist], string: CString): Ptr[curl_slist] = extern

  /**
    * Free an entire curl_slist.
    */
  def curl_slist_free_all(list: Ptr[curl_slist]): Unit = extern

  /**
    * Get the curl version string.
    */
  def curl_version(): CString = extern
}

/**
  * Callback functions and helpers for curl operations
  */
object CurlCallbacks {
  import CurlBindings.*

  /**
    * Write callback for collecting response body data
    */
  val writeCallback: CFuncPtr4[Ptr[Byte], CSize, CSize, Ptr[Byte], CSize] =
    (ptr: Ptr[Byte], size: CSize, nmemb: CSize, userdata: Ptr[Byte]) => {
      val realSize = size * nmemb
      val buffer   = userdata.asInstanceOf[Ptr[ResponseBuffer]]
      if (buffer != null && realSize > 0.toULong) {
        appendToBuffer(buffer, ptr, realSize.toLong)
      }
      realSize
    }

  /**
    * Header callback for collecting response headers
    */
  val headerCallback: CFuncPtr4[Ptr[Byte], CSize, CSize, Ptr[Byte], CSize] =
    (ptr: Ptr[Byte], size: CSize, nmemb: CSize, userdata: Ptr[Byte]) => {
      val realSize = size * nmemb
      val buffer   = userdata.asInstanceOf[Ptr[ResponseBuffer]]
      if (buffer != null && realSize > 0.toULong) {
        appendToBuffer(buffer, ptr, realSize.toLong)
      }
      realSize
    }

  private def appendToBuffer(buffer: Ptr[ResponseBuffer], data: Ptr[Byte], size: Long): Unit = {
    val currentSize = buffer._2
    val currentCap  = buffer._3
    val newSize     = currentSize + size

    if (newSize > currentCap) {
      // Need to grow the buffer
      val newCap    = math.max(newSize * 2, 1024L)
      val newData   = stdlib.malloc(newCap.toULong)
      val oldData   = buffer._1

      if (currentSize > 0 && oldData != null) {
        string.memcpy(newData, oldData, currentSize.toULong)
        stdlib.free(oldData)
      }

      buffer._1 = newData
      buffer._3 = newCap
    }

    string.memcpy(buffer._1 + currentSize, data, size.toULong)
    buffer._2 = newSize
  }

  /**
    * Buffer for accumulating response data. Structure: (data pointer, current size, capacity)
    */
  type ResponseBuffer = CStruct3[Ptr[Byte], CLong, CLong]

  def allocResponseBuffer(): Ptr[ResponseBuffer] = {
    val buffer = stdlib.malloc(sizeof[ResponseBuffer]).asInstanceOf[Ptr[ResponseBuffer]]
    buffer._1 = null
    buffer._2 = 0L
    buffer._3 = 0L
    buffer
  }

  def freeResponseBuffer(buffer: Ptr[ResponseBuffer]): Unit = {
    if (buffer != null) {
      if (buffer._1 != null) {
        stdlib.free(buffer._1)
      }
      stdlib.free(buffer.asInstanceOf[Ptr[Byte]])
    }
  }

  def getBufferData(buffer: Ptr[ResponseBuffer]): Array[Byte] = {
    if (buffer == null || buffer._1 == null || buffer._2 == 0L) {
      Array.empty[Byte]
    } else {
      val size  = buffer._2.toInt
      val bytes = new Array[Byte](size)
      var i     = 0
      while (i < size) {
        bytes(i) = !(buffer._1 + i)
        i += 1
      }
      bytes
    }
  }

  def getBufferString(buffer: Ptr[ResponseBuffer]): String = {
    new String(getBufferData(buffer), "UTF-8")
  }
}
