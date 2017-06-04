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

package wvlet.surface

trait Surface extends Serializable {
  def rawType: Class[_]
  def typeArgs: Seq[Surface]
  def params: Seq[Parameter]
  def name: String
  def fullName: String

  def dealias: Surface = this

  def isOption: Boolean
  def isAlias: Boolean
  def isPrimitive: Boolean

  def objectFactory: Option[ObjectFactory] = None
}


/**
  *
  */
object Surface {
  import scala.reflect.runtime.{universe => ru}

  // SurfaceFactory provides platform-specific Surface generator (e.g., ScalaJVM, JS)

  def of[A: ru.WeakTypeTag]: Surface = SurfaceFactory.of[A]
  def methodsOf[A: ru.WeakTypeTag]: Seq[MethodSurface] = SurfaceFactory.methodsOf[A]
}
