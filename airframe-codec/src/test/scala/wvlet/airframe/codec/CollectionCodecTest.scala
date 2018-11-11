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
package wvlet.airframe.codec

import wvlet.airframe.surface

import scala.collection.JavaConverters._

/**
  *
  */
class CollectionCodecTest extends CodecSpec {

  "CollectionCodec" should {
    "support Map type" in {
      val v = Map("id" -> 1)
      roundtrip(surface.of[Map[String, Int]], v, DataType.ANY)
    }

    "support Java Map type" in {
      val v = Map("id" -> 1).asJava
      roundtrip(surface.of[java.util.Map[String, Int]], v, DataType.ANY)
    }

    "support Seq/List type" in {
      roundtrip(surface.of[Seq[Int]], Seq(1, 2, 3), DataType.ANY)
      roundtrip(surface.of[List[Int]], List(1, 2, 3), DataType.ANY)
    }
  }

}
