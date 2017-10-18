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
package wvlet.airframe.tablet.msgpack

import wvlet.airframe.AirframeSpec
import wvlet.airframe.tablet.obj.{ObjectTabletReader, ObjectTabletWriter}
import wvlet.log.io.IOUtil

/**
  *
  */
class MessagePackTabletTest extends AirframeSpec {
  "MessagePackTablet" should {

    "write/read data in msgpack.gz" in {
      IOUtil.withTempFile("sample.msgpack.gz") { file =>
        val w = MessagePackTablet.msgpackGzWriter(file.getPath)
        new ObjectTabletReader(Seq(1, 2, 3)).pipe(w)
        w.close()

        val r  = MessagePackTablet.msgpackGzReader(file.getPath)
        val w2 = new ObjectTabletWriter[Int]
        val s  = r.pipe(w2).toSeq

        s shouldBe Seq(1, 2, 3)
      }
    }

  }
}
