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

import wvlet.airframe.surface.{MethodSurface, Surface}

trait RxRouter {
  def name: String

  def filter: Option[RxRouter.FilterNode]

  def routes: List[RxRouter]

  /**
    * Add a sibling router to this node
    *
    * @param router
    * @return
    */
  def add(router: RxRouter): RxRouter
  def +(router: RxRouter): RxRouter = add(router)
}

object RxRouter extends RxRouterObjectBase {

  def add(router: RxRouter): RxRouter = RxRouter.MultiNode(
    filter = None,
    routes = List(router)
  )

  /**
    * A collection of multiple routes
    * @param filter
    * @param routes
    */
  case class MultiNode(
      override val filter: Option[FilterNode] = None,
      override val routes: List[RxRouter]
  ) extends RxRouter {
    override def name: String = f"${this.hashCode()}%08x"
    override def add(router: RxRouter): RxRouter = {
      this.copy(routes = routes :+ router)
    }
  }

  /**
    * A single endpoint node
    * @param filter
    * @param controllerSurface
    * @param methodSurfaces
    */
  case class EndpointNode(
      override val filter: Option[FilterNode] = None,
      controllerSurface: Surface,
      methodSurfaces: Seq[MethodSurface]
  ) extends RxRouter {
    override def name: String = controllerSurface.name

    override def routes: List[RxRouter] = List.empty

    override def add(router: RxRouter): RxRouter = {
      MultiNode(filter = filter, routes = List(this, router))
    }
  }

  /**
    * Filter node of RxRouter
    * @param parent
    * @param filterSurface
    */
  case class FilterNode(
      parent: Option[FilterNode],
      filterSurface: Surface
  ) extends RxRouteFilterBase {
    def name: String = filterSurface.name

    def andThen(next: FilterNode): FilterNode = {
      next.copy(parent = Some(this))
    }

    def andThen(next: RxRouter): RxRouter = {
      next match {
        case r: MultiNode =>
          r.copy(filter = Some(this))
        case r: EndpointNode =>
          r.copy(filter = Some(this))
      }
    }
  }
}
