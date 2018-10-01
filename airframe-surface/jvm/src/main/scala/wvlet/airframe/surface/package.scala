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
package wvlet.airframe

import wvlet.airframe.surface.reflect.SurfaceFactory

import scala.reflect.runtime.{universe => ru}

/**
  *
  */
package object surface {

  def of[A: ru.WeakTypeTag]: Surface                   = SurfaceFactory.of[A]
  def methodsOf[A: ru.WeakTypeTag]: Seq[MethodSurface] = SurfaceFactory.methodsOf[A]

  def getCached(fullName: String): Surface = SurfaceFactory.get(fullName)
}
