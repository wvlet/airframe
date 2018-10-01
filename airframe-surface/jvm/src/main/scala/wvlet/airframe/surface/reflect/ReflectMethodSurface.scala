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
package wvlet.airframe.surface.reflect

import wvlet.airframe.surface.{MethodParameter, MethodSurface, Surface}
import java.{lang => jl}

import wvlet.log.LogSupport

import scala.util.Try

/**
  * MethodSurface for JVM. This can call method through Java reflection
  */
case class ReflectMethodSurface(mod: Int, owner: Surface, name: String, returnType: Surface, args: Seq[MethodParameter])
    extends MethodSurface
    with LogSupport {

  private lazy val method: Option[jl.reflect.Method] = {
    Try(owner.rawType.getDeclaredMethod(name, args.map(_.surface.rawType): _*)).toOption
  }

  def getMethod: Option[jl.reflect.Method] = method

  override def call(obj: Any, x: Any*): Any = method match {
    case Some(m) =>
      if (x == null || x.isEmpty) {
        m.invoke(obj)
      } else {
        m.invoke(obj, x.map(_.asInstanceOf[AnyRef]): _*)
      }
    case None => null
  }
}
