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
package wvlet.airframe.http

import wvlet.airframe.surface.{MethodSurface, Surface}

case class RxFilterDef(filter: RxFilter, filterSurface: Surface) {
  def name: String = filterSurface.rawType.getClass.getSimpleName
}

case class RxRouter(
    parent: Option[RxRouter] = None,
    controllerSurface: Option[Surface] = None,
    methodSurfaces: Seq[MethodSurface] = Seq.empty,
    siblings: Seq[RxRouter] = Seq.empty,
    filter: Option[RxFilterDef] = None
) extends RxRouterBase {

  def withParent(parent: RxRouter): RxRouter = {
    this.copy(parent = Some(parent))
  }

  def name: String = {
    controllerSurface
      .orElse(filter.map(_.name))
      .getOrElse(f"${hashCode()}%x")
      .toString
  }

  def andThen(next: RxRouter): RxRouter = {
    siblings.size match {
      case s if s <= 1 =>
        next.withParent(this)
      case other =>
        throw new IllegalStateException(
          s"Cannot add child router ${next.name} to ${this.name} if it already has multiple siblings"
        )
    }
  }

  def add(router: RxRouter): RxRouter = {
    this.copy(siblings = siblings :+ router.withParent(this))
  }

  def addInternal(controllerSurface: Surface, methodSurfaces: Seq[MethodSurface]): RxRouter = {
    add(RxRouter(None, Some(controllerSurface), methodSurfaces))
  }
}

object RxRouter extends RxRouterObjectBase {
  def empty: RxRouter = RxRouter(None)

  def merge(routers: RxRouter*): RxRouter = {
    routers.toSeq.reduce { (r1, r2) => r1.add(r2) }
  }
}
