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
package wvlet.surface.reflect

import wvlet.surface.{MethodRef, Parameter, Surface}
import java.{lang => jl}

import wvlet.log.LogSupport

/**
  *
  */
case class RuntimeMethodParameter(
  method: MethodRef,
  index: Int,
  name: String,
  surface: Surface
)
  extends Parameter with LogSupport {
  override def toString: String = s"${name}:${surface.name}"

  private lazy val field: jl.reflect.Field = method.owner.getDeclaredField(name)

  def get(x: Any): Any = {
    try {
      ReflectTypeUtil.readField(x, field)
    }
    catch {
      case e: IllegalAccessException =>
        error(s"read field: class ${surface.rawType}, field:${field.getName}", e)
        throw e
    }
  }

  def getDefaultValue: Option[Any] = {
    ReflectTypeUtil.companionObject(method.owner).flatMap {companion =>
      def findMethod(name: String) = {
        try {
          Some(ReflectTypeUtil.cls(companion).getDeclaredMethod(name))
        }
        catch {
          case e: NoSuchMethodException => None
        }
      }
      // Find Scala methods for retrieving default values. Since Scala 2.10 appply or $lessinit$greater$ can be the prefix
      val m =
        findMethod("apply$default$%d".format(index + 1))
        .orElse(findMethod("$lessinit$greater$default$%d".format(index + 1)))
      try {
        m.map(_.invoke(companion))
      }
      catch {
        case e: Throwable =>
          None
      }
    }
  }
}
