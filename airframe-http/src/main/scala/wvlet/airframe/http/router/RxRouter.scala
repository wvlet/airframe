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

  def filter: Option[RxRouteFilter]
  def routes: List[RxRouter]

  /**
    * Add a sibling router to this node
    * @param router
    * @return
    */
  def add(router: RxRouter): RxRouter = ???
  def +(router: RxRouter): RxRouter   = add(router)
}

case class RxRouteFilter(
  parent: Option[RxRouteFilter],
  filterSurface: Surface
)
  extends RxRouteFilterBase
{
  def name: String = filterSurface.name

  def andThen(next: RxRouteFilter): RxRouteFilter =
  {
    next.copy(parent = Some(this))
  }

  def andThen(next: RxEndpointNode): RxRouter =
  {
    next.copy(filter = Some(this))
  }
}

object RxRouter extends RxRouterObjectBase {

  def add(router: RxRouter): RxRouter = RxRouterLeaf(
    filter = None,
    routes = List(router)
  )


  case class RxRouteCollection(
      override val filter: Option[RxRouteFilter] = None,
      routes: List[RxRouter]
  ) extends RxRouter {
    // TODO
    override def name: String = ???
  }

  case class RxEndpointNode(
      override val filter: Option[RxRouteFilter] = None,
      controllerSurface: Surface,
      methodSurfaces: Seq[MethodSurface]
  ) extends RxRouter {
    override def name: String = controllerSurface.name

    override def routes: List[RxRouter] = List.empty

    override def add(router: RxRouter): RxRouter = {
      RxRouterLeaf(
        filter = this.filter,
        routes = List(this, router)
      )
    }
  }
}
