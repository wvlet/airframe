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
object CurlBindings {

  /**
    * Opaque handle for a curl easy session
    */
  type CURL = Ptr[Byte]

  /**
    * Linked list structure for headers and other string lists
    */
  type CurlSlist = CStruct2[CString, Ptr[Byte]]

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
  final val CURL_GLOBAL_DEFAULT: Long = 3L

  // CURLOPT options (using actual libcurl values)
  final val CURLOPT_URL: CInt               = 10002
  final val CURLOPT_POSTFIELDS: CInt        = 10015
  final val CURLOPT_CUSTOMREQUEST: CInt     = 10036
  final val CURLOPT_WRITEDATA: CInt         = 10001
  final val CURLOPT_HEADERDATA: CInt        = 10029
  final val CURLOPT_HTTPHEADER: CInt        = 10023
  final val CURLOPT_CONNECTTIMEOUT_MS: CInt = 156
  final val CURLOPT_TIMEOUT_MS: CInt        = 155
  final val CURLOPT_FOLLOWLOCATION: CInt    = 52
  final val CURLOPT_MAXREDIRS: CInt         = 68
  final val CURLOPT_POST: CInt              = 47
  final val CURLOPT_POSTFIELDSIZE: CInt     = 60
  final val CURLOPT_HTTPGET: CInt           = 80
  final val CURLOPT_NOBODY: CInt            = 44
  final val CURLOPT_HTTP_VERSION: CInt      = 84
  final val CURLOPT_WRITEFUNCTION: CInt     = 20011
  final val CURLOPT_HEADERFUNCTION: CInt    = 20079

  // CURLINFO codes
  final val CURLINFO_RESPONSE_CODE: CInt = 0x200002

  // HTTP version options
  final val CURL_HTTP_VERSION_1_1: Long = 2L

  @link("curl")
  @extern
  private[client] object Extern {
    @name("curl_global_init")
    def globalInit(flags: Long): CInt = extern

    @name("curl_global_cleanup")
    def globalCleanup(): Unit = extern

    @name("curl_easy_init")
    def easyInit(): CURL = extern

    @name("curl_easy_cleanup")
    def easyCleanup(curl: CURL): Unit = extern

    @name("curl_easy_perform")
    def easyPerform(curl: CURL): CInt = extern

    @name("curl_easy_setopt")
    def easySetoptLong(curl: CURL, option: CInt, parameter: Long): CInt = extern

    @name("curl_easy_setopt")
    def easySetoptPtr(curl: CURL, option: CInt, parameter: Ptr[Byte]): CInt = extern

    @name("curl_easy_setopt")
    def easySetoptCallback(curl: CURL, option: CInt, parameter: CFuncPtr4[Ptr[Byte], CSize, CSize, Ptr[Byte], CSize]): CInt = extern

    @name("curl_easy_setopt")
    def easySetoptSlist(curl: CURL, option: CInt, parameter: Ptr[CurlSlist]): CInt = extern

    @name("curl_easy_getinfo")
    def easyGetinfoLong(curl: CURL, info: CInt, result: Ptr[Long]): CInt = extern

    @name("curl_easy_strerror")
    def easyStrerror(code: CInt): CString = extern

    @name("curl_slist_append")
    def slistAppend(list: Ptr[CurlSlist], string: CString): Ptr[CurlSlist] = extern

    @name("curl_slist_free_all")
    def slistFreeAll(list: Ptr[CurlSlist]): Unit = extern

    @name("curl_version")
    def version(): CString = extern
  }

  // Convenient wrapper functions
  def curl_global_init(flags: Long): CInt = Extern.globalInit(flags)
  def curl_global_cleanup(): Unit         = Extern.globalCleanup()
  def curl_easy_init(): CURL              = Extern.easyInit()
  def curl_easy_cleanup(curl: CURL): Unit = Extern.easyCleanup(curl)
  def curl_easy_perform(curl: CURL): CInt = Extern.easyPerform(curl)

  def curl_easy_setopt_long(curl: CURL, option: CInt, parameter: Long): CInt =
    Extern.easySetoptLong(curl, option, parameter)

  def curl_easy_setopt_ptr(curl: CURL, option: CInt, parameter: Ptr[Byte]): CInt =
    Extern.easySetoptPtr(curl, option, parameter)

  def curl_easy_setopt_str(curl: CURL, option: CInt, parameter: CString): CInt =
    Extern.easySetoptPtr(curl, option, parameter.asInstanceOf[Ptr[Byte]])

  def curl_easy_setopt_callback(
      curl: CURL,
      option: CInt,
      parameter: CFuncPtr4[Ptr[Byte], CSize, CSize, Ptr[Byte], CSize]
  ): CInt = Extern.easySetoptCallback(curl, option, parameter)

  def curl_easy_setopt_slist(curl: CURL, option: CInt, parameter: Ptr[CurlSlist]): CInt =
    Extern.easySetoptSlist(curl, option, parameter)

  def curl_easy_getinfo_long(curl: CURL, info: CInt, result: Ptr[Long]): CInt =
    Extern.easyGetinfoLong(curl, info, result)

  def curl_easy_strerror(code: CInt): CString       = Extern.easyStrerror(code)
  def curl_slist_append(list: Ptr[CurlSlist], str: CString): Ptr[CurlSlist] = Extern.slistAppend(list, str)
  def curl_slist_free_all(list: Ptr[CurlSlist]): Unit = Extern.slistFreeAll(list)
  def curl_version(): CString = Extern.version()
}

/**
  * Callback functions and helpers for curl operations
  */
object CurlCallbacks {
  import CurlBindings.*

  /**
    * Write callback type for curl
    */
  type WriteCallback = CFuncPtr4[Ptr[Byte], CSize, CSize, Ptr[Byte], CSize]

  /**
    * Write callback for collecting response data
    */
  val writeCallback: WriteCallback = {
    (ptr: Ptr[Byte], size: CSize, nmemb: CSize, userdata: Ptr[Byte]) =>
      val realSize = size * nmemb
      if (userdata != null && realSize.toLong > 0L) {
        val buffer = userdata.asInstanceOf[Ptr[ResponseBuffer]]
        appendToBuffer(buffer, ptr, realSize.toLong)
      }
      realSize
  }

  /**
    * Header callback for collecting response headers
    */
  val headerCallback: WriteCallback = {
    (ptr: Ptr[Byte], size: CSize, nmemb: CSize, userdata: Ptr[Byte]) =>
      val realSize = size * nmemb
      if (userdata != null && realSize.toLong > 0L) {
        val buffer = userdata.asInstanceOf[Ptr[ResponseBuffer]]
        appendToBuffer(buffer, ptr, realSize.toLong)
      }
      realSize
  }

  private def appendToBuffer(buffer: Ptr[ResponseBuffer], data: Ptr[Byte], size: Long): Unit = {
    val currentSize = buffer._2
    val currentCap  = buffer._3
    val newSize     = currentSize + size

    if (newSize > currentCap) {
      val newCap  = math.max(newSize * 2, 1024L)
      val newData = stdlib.malloc(newCap)
      val oldData = buffer._1

      if (currentSize > 0 && oldData != null) {
        string.memcpy(newData, oldData, currentSize.toCSize)
        stdlib.free(oldData)
      }

      buffer._1 = newData
      buffer._3 = newCap
    }

    string.memcpy(buffer._1 + currentSize, data, size.toCSize)
    buffer._2 = newSize
  }

  /**
    * Buffer for accumulating response data. Structure: (data pointer, current size, capacity)
    */
  type ResponseBuffer = CStruct3[Ptr[Byte], Long, Long]

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
