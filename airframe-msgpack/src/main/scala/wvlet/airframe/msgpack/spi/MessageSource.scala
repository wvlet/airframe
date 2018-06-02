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
package wvlet.airframe.msgpack.spi

import java.io.IOException

/**
  *
  */
trait MessageSource extends AutoCloseable {

  /**
    * Returns a next buffer to read.
    * <p>
    * This method should return a [[ReadBuffer]] instance that has data filled in. When this method is called twice,
    * the previously returned buffer is no longer used. Thus implementation of this method can safely discard it.
    * This is useful when it uses a memory pool.
    *
    * @return the next input [[ReadBuffer]], or return None if no more buffer is available.
    * @throws IOException when IO error occurred when reading the data
    */
  def next: Option[ReadBuffer]

  /**
    * Closes the input.
    * <p>
    * When this method is called, the buffer previously returned from [[next]] method is no longer used.
    * Thus implementation of this method can safely discard it.
    * <p>
    * If the input is already closed then invoking this method has no effect.
    *
    * @throws Exception when IO error occurred when closing the data source
    */
  override def close: Unit
}
