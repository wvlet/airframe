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

import wvlet.airframe.http.RxFilter
import wvlet.airframe.surface.{MethodSurface, Surface}

trait RxRouter {
  def name: String
  def parent: Option[RxRoute]

  def isRoot: Boolean = parent.isEmpty
  def root: RxRouter = {
    parent.map(_.root).getOrElse(this)
  }

  /**
    * Add a sibling router to this node
    * @param router
    * @return
    */
  def add(router: RxRouter): RxRouter = ???

  def +(router: RxRouter): RxRouter = add(router)
}

case class RxRoute(
    filter: List[RxFilter] = Nil,
    children: List[RxRouter] = Nil,
)


object RxRouter extends RxRouterObjectBase {
  def add(router: RxRouter): RxRouter = ???


  case class RxRouteFilter(
    parent: Option[RxRouteFilter],
    filterSurface: Surface
  ) {
    def name: String = filterSurface.name

    def withParent(parent: RxRouteFilter): RxRouteFilter ={
      this.copy(parent = Some(parent))
    }
    def andThen(next: RxRouteFilter): RxRouteFilter = {
      next.copy(parent = Some(this))
    }
    def andThen(next: EndpointNode): EndpointNode = {
      next.copy(parent = Some(this))
    }
  }

  case class EndpointNode(
      override val parent: Option[RxRouteFilter],
      controllerSurface: Surface,
      methodSurfaces: Seq[MethodSurface]
  ) extends RxRouter {
    override def name: String    = controllerSurface.name
    override def isLeaf: Boolean = true
    override def isNode: Boolean = false

    override def children: Seq[RxRouter] = Seq.empty
  }

//  def merge(routers: RxRouter*): RxRouter = {
//    routers.toSeq.reduce { (r1, r2) => r1.add(r2) }
//  }
}
