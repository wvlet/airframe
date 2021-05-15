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

import wvlet.airspec.AirSpec

/**
  */
class OffHeapMemoryAllocatorTest extends AirSpec {
  test("allocate and release memory") {
    val a  = new OffHeapMemoryAllocator
    val m1 = a.allocate(10)
    a.allocatedMemorySize shouldBe 10
    val m2 = a.allocate(100)
    a.allocatedMemorySize shouldBe 110
    a.release(m1)
    a.allocatedMemorySize shouldBe 100
    a.close()
    a.allocatedMemorySize shouldBe 0
  }
}
