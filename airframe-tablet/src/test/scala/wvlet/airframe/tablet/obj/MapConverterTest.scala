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
package wvlet.airframe.tablet.obj

import wvlet.airframe.spec.AirSpec
import wvlet.airframe.tablet.obj.MapConverterTest.Sample

object MapConverterTest {
  case class Sample(name: String, id: Int)
}

/**
  *
  */
class MapConverterTest extends AirSpec {
  def `convert to Map`: Unit = {
    val s  = Sample("leo", 1)
    val mc = MapConverter.of[Sample]
    val m  = mc.toMap(s)
    debug(m)
  }
}
