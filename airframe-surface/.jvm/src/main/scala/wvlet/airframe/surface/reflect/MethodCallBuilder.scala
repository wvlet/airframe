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

/**
  */
import wvlet.airframe.surface.CanonicalNameFormatter._
import wvlet.airframe.surface.{CName, MethodParameter, MethodSurface, Zero}
import wvlet.log.LogSupport

//--------------------------------------
//
// MethodCallBuilder.scala
// Since: 2012/03/27 16:43
//
//--------------------------------------

/**
  * Builds method call arguments
  *
  * @author
  *   leo
  */
class MethodCallBuilder(m: MethodSurface, owner: AnyRef) extends StandardBuilder with LogSupport {
  // Find the default arguments of the method
  protected def defaultValues =
    (for (p <- m.args; v <- findDefaultValue(p.name)) yield {
      p.name.canonicalName -> v
    }).toMap

  protected def findParameter(name: String): Option[MethodParameter] = {
    val cname = CName(name)
    m.args.find(p => CName(p.name) == cname)
  }

  private def findDefaultValue(name: String): Option[Any] = {
    findParameter(name).flatMap { p =>
      try {
        val methodName = "%s$default$%d".format(m.name, p.index + 1)
        val dm         = owner.getClass.getMethod(methodName)
        Some(dm.invoke(owner))
      } catch {
        case _: Throwable => None
      }
    }
  }

  def execute: Any = {
    trace(s"holder: $holder")
    val args = for (p <- m.args) yield {
      get(p.name).getOrElse(Zero.zeroOf(p.surface))
    }
    m.call(owner, args: _*)
  }
}
