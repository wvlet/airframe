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

import wvlet.log.LogSupport

import scala.jdk.CollectionConverters.*

private[canvas] case class MemoryRefHolder(ref: MemoryReference, size: Long)

/**
  * CanvasAllocator is responsible for creating new Canvases and manages allocate memory references.
  */
class OffHeapMemoryAllocator extends AutoCloseable with LogSupport {
  type Address = Long

  private val allocatedMemoryAddresses = new ConcurrentHashMap[Address, MemoryRefHolder]().asScala
  private val memoryQueue              = new ReferenceQueue[Memory]()
  private val totalAllocatedMemorySize = new AtomicLong(0)

  {
    // Start a off-heap memory collector to release allocated memory upon GC
    val collectorThread = new Thread(new Runnable {
      override def run(): Unit = {
        val ref = classOf[MemoryReference].cast(memoryQueue.remove())
        release(ref)
      }
    })
    collectorThread.setName("AirframeCanvas-GC")
    collectorThread.setDaemon(true) // Allow shutting down JVM while this thread is running
    logger.debug("Start Canvas-GC memory collector")
    collectorThread.start()
  }

  def allocatedMemorySize: Long = {
    totalAllocatedMemorySize.get()
  }

  def allocate(size: Long): OffHeapMemory = {
    val address = UnsafeUtil.unsafe.allocateMemory(size)
    val m       = OffHeapMemory(address, size, this)
    register(m)
    m
  }

  private def register(m: OffHeapMemory): Unit = {
    // Register a memory reference that will be collected by GC
    val ref    = new MemoryReference(m, memoryQueue)
    val holder = MemoryRefHolder(ref, m.size)
    allocatedMemoryAddresses.put(m.address, holder)
    totalAllocatedMemorySize.addAndGet(m.size)
  }

  private[canvas] def release(m: OffHeapMemory): Unit = {
    releaseMemoryAt(m.address)
  }

  private[canvas] def release(reference: MemoryReference): Unit = {
    releaseMemoryAt(reference.address)
  }

  private def releaseMemoryAt(address: Long): Unit = {
    debug(f"Releasing memory at ${address}%x")
    allocatedMemoryAddresses.get(address) match {
      case Some(h) =>
        if address != 0 then {
          UnsafeUtil.unsafe.freeMemory(address)
          totalAllocatedMemorySize.getAndAdd(-h.size)
        }
        allocatedMemoryAddresses.remove(address)
      case None =>
        warn(f"Unknown allocated memory address: ${address}%x")
    }
  }

  override def close(): Unit = {
    for address <- allocatedMemoryAddresses.keys do {
      releaseMemoryAt(address)
    }
  }
}
