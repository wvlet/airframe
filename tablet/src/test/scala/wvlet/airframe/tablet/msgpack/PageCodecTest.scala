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

/**
  *
  */
import wvlet.airframe.tablet.Page
import wvlet.airframe.tablet.msgpack.PageCodecTest._
class PageCodecTest extends CodecSpec {
  "PageCodec" should {

    "encode Seq[A] as a Page" in {
      val pageCodec = MessageCodec.pageCodecOf[R]

      val data = Seq(R(1, "Leo"), R(1, "Aina"))
      Page()

    }

  }
}

object PageCodecTest {

  case class R(id: Int, name: String)

}
