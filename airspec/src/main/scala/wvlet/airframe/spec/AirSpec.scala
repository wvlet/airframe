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
package wvlet.airframe.spec

import wvlet.airframe.Design
import wvlet.airframe.surface.reflect.{ReflectMethodSurface, ReflectSurfaceFactory}
import wvlet.airframe.surface.{MethodSurface, Surface}

/**
  * A base trait for defining unit tests using airspec
  */
trait AirSpec {
  private[spec] def surface: Surface = {
    ReflectSurfaceFactory.ofClass(this.getClass)
  }
  private[spec] def methodSurfaces: Seq[MethodSurface] = {
    ReflectSurfaceFactory.methodsOfClass(this.getClass)
  }

  protected def design: Design = Design.empty
}
