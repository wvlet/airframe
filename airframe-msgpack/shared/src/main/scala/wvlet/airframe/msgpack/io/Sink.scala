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
package wvlet.airframe.msgpack.io

import java.io.{Flushable, IOException}

/**
  * Provides a buffered output stream that writes sequence of [[WriteBuffer]] instances.
  *
  * A Sink implementation has total control of the buffer memory so that it can reuse buffer memory,
  * use buffer pools, or use memory-mapped files.
  */
trait Sink extends AutoCloseable with Flushable {

  /**
    * Allocates the next buffer for writing MessagePack data.
    * <p>
    * This method returns an [[WriteBuffer]] instance that has specified size of capacity at least.
    * <p>
    * When this method is called twice, the previously returned buffer is no longer used. This method may be called
    * twice without call of [[writeBuffer(int)]] in between. In this case, the buffer should be
    * discarded without flushing it to the output.
    *
    * @param minimumSize the mimium required buffer size to allocate
    * @return a Buffer instance with at least minimumSize bytes of capacity
    * @throws IOException
    */
  @throws[IOException]
  def next(minimumSize: Int): WriteBuffer

  /**
    * Writes the previously allocated buffer(s).
    * <p>
    * This method should write the buffer previously returned from [[next(int)]] method until specified number of
    * bytes. Once the entire buffer contents is totally written to the sink, the buffer should not be used because
    * a BufferSink implementation might reuse the buffer.
    * <p>
    * This method is not always called for each [[next(int)]] call. In this case, the buffer should be discarded
    * without flushing it to the output when the next [[next(int)]] is called.
    *
    * @param length the number of bytes to write
    * @throws IOException
    */
  @throws[IOException]
  def writeBuffer(length: Int): Unit

  /**
    * Writes an external payload data.
    * This method should follow semantics of OutputStream.
    *
    * @param buffer the data to write
    * @param offset the start offset in the data
    * @param length the number of bytes to write
    * @throws IOException
    */
  @throws[IOException]
  def write(buffer: Array[Byte], offset: Int, length: Int): Unit

  /**
    * Writes an external payload data.
    * <p>
    * Unlike [[write(byte[], int, int)]] method, the buffer is given - this BufferSink implementation
    * gets ownership of the buffer and may modify contents of the buffer. Contents of this buffer won't be modified
    * by the caller.
    *
    * @param buffer the data to add
    * @param offset the start offset in the data
    * @param length the number of bytes to add
    * @throws IOException
    */
  @throws[IOException]
  def add(buffer: Array[Byte], offset: Int, length: Int): Unit

}
