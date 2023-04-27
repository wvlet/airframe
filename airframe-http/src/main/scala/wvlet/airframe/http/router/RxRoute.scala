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
package wvlet.airframe.http.router

import wvlet.airframe.http.RxRouter.FilterNode
import wvlet.airframe.surface.{MethodSurface, Surface}

case class RxRoute(filter: Option[FilterNode], controllerSurface: Surface, methodSurfaces: Seq[MethodSurface]) {
  override def toString: String = {
    val s = Seq.newBuilder[String]
    for (m <- methodSurfaces) {
      s += s"${m.name}(${m.args.map(x => s"${x.name}:${x.surface}").mkString(", ")}): ${m.returnType}"
    }
    s.result().mkString("\n")
  }

  def wrapWithFilter(parentFilter: Option[FilterNode]): RxRoute = {
    this.copy(filter = parentFilter match {
      case None    => filter
      case Some(f) => f.andThenOpt(filter)
    })
  }
}
