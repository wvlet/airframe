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
package wvlet.airframe.examples.surface

import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

/**
  */
object Surface_01_Basic extends App with LogSupport {
  case class MyConfig(port: Int = 8080, name: String)

  val surface = Surface.of[MyConfig]

  val p0 = surface.params(0)
  info(p0.name)            // port
  info(p0.surface)         // Primitive.Int
  info(p0.getDefaultValue) // Some(8080)

  val p1 = surface.params(1)
  info(p1.name)            // name
  info(p1.surface)         // Primitive.String
  info(p1.getDefaultValue) // None

  val obj = surface.objectFactory.map { f => f.newInstance(Seq(10010, "hello")) }
  info(obj) // Some(MyConfig(10010, hello))
}
