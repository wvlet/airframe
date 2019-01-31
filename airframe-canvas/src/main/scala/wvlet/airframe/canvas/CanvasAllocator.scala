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
package wvlet.airframe.canvas
import java.lang.ref.ReferenceQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

import scala.collection.JavaConverters._

/**
  * CanvasAllocator is responsible for creating new Canvases and manages allocate memory references.
  */
class CanvasAllocator extends AutoCloseable {
  type Address = Long
  type Size    = Long

  private val allocatedMemoryAddresses = new ConcurrentHashMap[Address, Size]().asScala
  private val memoryQueue              = new ReferenceQueue[Memory]()
  private val totalAllocatedMemorySize = new AtomicLong(0)

  def allocate(size: Long): Memory = {}

  override def close(): Unit = {}
}
