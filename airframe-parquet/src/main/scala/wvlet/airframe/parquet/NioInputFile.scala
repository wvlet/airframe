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
package wvlet.airframe.parquet

import org.apache.parquet.io.{InputFile, SeekableInputStream}

import java.io.IOException
import java.nio.channels.SeekableByteChannel
import java.nio.file.{Files, Path}
import java.nio.{ByteBuffer, ByteOrder}

/**
  * An [[org.apache.parquet.io.InputFile]] implementation that only uses java.nio.* interfaces for I/O operations. The
  * LocalInputFile implementation in upstream parquet-mr currently falls back to the old-school java file I/O APIs (via
  * Path#toFile) which won't work with nio remote FileSystems such as an S3 FileSystem implementation.
  *
  * Based on: https://blakesmith.me/2024/10/05/how-to-use-parquet-java-without-hadoop.html
  *
  * @param path
  */
class NioInputFile(path: Path) extends InputFile {
  private var length: Long = -1L

  override def getLength: Long = {
    if (length == -1L) {
      length = Files.size(path)
    }
    length
  }

  override def newStream(): SeekableInputStream = {
    new SeekableInputStream() {
      private val byteChannel: SeekableByteChannel = Files.newByteChannel(path)
      private val singleByteBuffer: ByteBuffer     = ByteBuffer.allocate(1)

      override def read(): Int = {
        singleByteBuffer.clear()
        val numRead = read(singleByteBuffer)
        if (numRead >= 0) {
          val value = singleByteBuffer.get(0) & 0xff
          value
        } else {
          -1
        }
      }

      override def getPos: Long = byteChannel.position()

      override def seek(newPos: Long): Unit = {
        byteChannel.position(newPos)
        ()
      }

      override def readFully(bytes: Array[Byte]): Unit = {
        readFully(bytes, 0, bytes.length)
      }

      override def readFully(bytes: Array[Byte], start: Int, len: Int): Unit = {
        val buf = ByteBuffer.wrap(bytes)
        buf.position(start)
        buf.limit(start + len)
        readFully(buf)
      }

      override def read(buf: ByteBuffer): Int = {
        byteChannel.read(buf)
      }

      override def readFully(buf: ByteBuffer): Unit = {
        var numRead = 0
        while (numRead < buf.limit()) {
          val code = read(buf)
          if (code == -1) {
            return
          } else {
            numRead += code
          }
        }
      }

      override def close(): Unit = {
        byteChannel.close()
      }
    }
  }
}
