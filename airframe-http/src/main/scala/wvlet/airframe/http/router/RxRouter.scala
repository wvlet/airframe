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

import wvlet.airframe.http.router.RxRouter.FilterNode
import wvlet.airframe.surface.{MethodSurface, Surface}

trait RxRouter {
  def name: String
  def filter: Option[FilterNode]
  def children: List[RxRouter]

  def routes: List[RxRoute]

  /**
    * Add a sibling router to this node
    *
    * @param router
    * @return
    */
  def add(router: RxRouter): RxRouter
  def +(router: RxRouter): RxRouter = add(router)

  def wrapWithFilter(parentFilter: FilterNode): RxRouter

  override def toString: String = printNode(0)

  private def printNode(indentLevel: Int): String = {
    val s  = Seq.newBuilder[String]
    val ws = " " * (indentLevel * 2)
    s += s"${ws}- Router[${name}]"

    for (c <- children) {
      s += c.printNode(indentLevel + 1)
    }
    s.result().mkString("\n")
  }
}

case class RxRoute(filter: Option[FilterNode], controllerSurface: Surface, methodSurfaces: Seq[MethodSurface]) {
  def wrapWithFilter(parentFilter: Option[FilterNode]): RxRoute = {
    this.copy(filter = parentFilter match {
      case None    => filter
      case Some(f) => f.andThenOpt(filter)
    })
  }
}

object RxRouter extends RxRouterObjectBase {

  def add(router: RxRouter): RxRouter = RxRouter.StemNode(
    filter = None,
    children = List(router)
  )

  /**
    * A collection of multiple routes
    * @param filter
    * @param children
    */
  case class StemNode(
      override val filter: Option[FilterNode] = None,
      override val children: List[RxRouter]
  ) extends RxRouter {
    override def name: String = f"${this.hashCode()}%08x"
    override def add(router: RxRouter): RxRouter = {
      this.copy(children = children :+ router)
    }
    override def wrapWithFilter(parentFilter: FilterNode): RxRouter = {
      this.copy(filter = parentFilter.andThenOpt(filter))
    }

    override def routes: List[RxRoute] = {
      children.flatMap { c =>
        c.routes.map { r =>
          r.wrapWithFilter(filter)
        }
      }
    }
  }

  /**
    * A single endpoint node without any filter
    * @param filter
    * @param controllerSurface
    * @param methodSurfaces
    */
  case class EndpointNode(
      controllerSurface: Surface,
      methodSurfaces: Seq[MethodSurface]
  ) extends RxRouter {
    override def name: String               = controllerSurface.name
    override def filter: Option[FilterNode] = None
    override def children: List[RxRouter]   = Nil
    override def add(router: RxRouter): RxRouter = {
      StemNode(children = List(this, router))
    }
    override def wrapWithFilter(parentFilter: FilterNode): RxRouter = {
      StemNode(filter = Some(parentFilter), children = List(this))
    }
    override def routes: List[RxRoute] =
      List(RxRoute(None, controllerSurface, methodSurfaces))
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
    def andThenOpt(next: Option[FilterNode]): Option[FilterNode] = {
      next match {
        case Some(f) => Some(this.andThen(f))
        case None    => Some(this)
      }
    }
    def andThen(next: RxRouter): RxRouter = {
      next.wrapWithFilter(this)
    }
  }
}
