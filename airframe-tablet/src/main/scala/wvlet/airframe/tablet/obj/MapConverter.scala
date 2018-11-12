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

import wvlet.airframe.surface.Surface

import scala.reflect.runtime.{universe => ru}

class MapConverter[A](surface: Surface) {
  def toMap(a: A): Map[String, Any] = {
    val m = Map.newBuilder[String, Any]
    for (p <- surface.params) {
      m += (p.name -> p.get(a))
    }
    m.result()
  }
}

object MapConverter {
  def of[A: ru.TypeTag]: MapConverter[A] = new MapConverter(Surface.of[A])

  def toMap[A: ru.TypeTag](a: A): Map[String, Any] = {
    MapConverter.of[A].toMap(a)
  }
}
