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
package wvlet.airframe.http.netty

import wvlet.airframe.ulid.ULID
import wvlet.airspec.AirSpec

class NettyBackendTest extends AirSpec {
  test("thread local storage") {

    val key = ULID.newULIDString

    test("must be None by default") {
      NettyBackend.getThreadLocal[Int](key) shouldBe None
    }

    test("store different content for each thread") {
      NettyBackend.setThreadLocal[Int](key, 123)

      var valueInThread: Option[Int] = None

      val t = new Thread {
        override def run(): Unit = {
          NettyBackend.getThreadLocal[Int](key) shouldBe None
          NettyBackend.setThreadLocal[Int](key, 456)
          valueInThread = NettyBackend.getThreadLocal[Int](key)
        }
      }
      t.start()
      t.join()

      NettyBackend.getThreadLocal[Int](key) shouldBe Some(123)
      valueInThread shouldBe Some(456)
    }
  }
}
