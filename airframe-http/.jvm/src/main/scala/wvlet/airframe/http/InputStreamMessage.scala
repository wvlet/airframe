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
package wvlet.airframe.http

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.nio.charset.StandardCharsets

/**
  * A Message implementation backed by an InputStream. This allows handlers to read large request bodies without holding
  * the entire content in memory.
  *
  * The InputStream can only be consumed once via getInputStream. If toContentBytes or toContentString is called, the
  * stream is fully read and cached. If getInputStream is called first, a tee-reading wrapper is returned that caches
  * bytes as they are read, so toContentBytes can still work afterwards.
  */
class InputStreamMessage(inputStream: InputStream) extends HttpMessage.Message {
  private var cachedBytes: Array[Byte] = null

  private def ensureCached(): Array[Byte] = synchronized {
    if (cachedBytes == null) {
      cachedBytes = inputStream.readAllBytes()
    }
    cachedBytes
  }

  /**
    * Returns an InputStream for reading the body content. If the content has already been materialized via
    * toContentBytes/toContentString, returns a ByteArrayInputStream over the cached data. Otherwise, returns a wrapper
    * that caches bytes as they are read.
    */
  def getInputStream: InputStream = synchronized {
    if (cachedBytes != null) {
      new ByteArrayInputStream(cachedBytes)
    } else {
      new TeeInputStream(inputStream, this)
    }
  }

  private[http] def setCachedBytes(bytes: Array[Byte]): Unit = synchronized {
    if (cachedBytes == null) {
      cachedBytes = bytes
    }
  }

  override def isEmpty: Boolean            = false
  override def toContentBytes: Array[Byte] = ensureCached()
  override def toContentString: String     = new String(ensureCached(), StandardCharsets.UTF_8)
}

/**
  * An InputStream wrapper that caches all bytes read from the underlying stream into a buffer. When the stream is fully
  * consumed, the cached bytes are stored back into the parent InputStreamMessage for subsequent access via
  * toContentBytes.
  */
private[http] class TeeInputStream(underlying: InputStream, parent: InputStreamMessage) extends InputStream {
  private val buffer = new ByteArrayOutputStream()

  override def read(): Int = {
    val b = underlying.read()
    if (b >= 0) {
      buffer.write(b)
    } else {
      parent.setCachedBytes(buffer.toByteArray)
    }
    b
  }

  override def read(b: Array[Byte], off: Int, len: Int): Int = {
    val n = underlying.read(b, off, len)
    if (n > 0) {
      buffer.write(b, off, n)
    } else if (n < 0) {
      parent.setCachedBytes(buffer.toByteArray)
    }
    n
  }

  override def available(): Int = underlying.available()

  override def close(): Unit = {
    // Cache whatever has been read so far (do not drain remaining bytes to avoid memory issues)
    parent.setCachedBytes(buffer.toByteArray)
    underlying.close()
  }
}
