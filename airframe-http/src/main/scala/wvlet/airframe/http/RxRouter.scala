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

import wvlet.airframe.http.router.{RedirectToRxEndpoint, RxRoute}
import wvlet.airframe.surface.{MethodSurface, Surface}

trait RxRouter {
  def name: String
  def filter: Option[RxRouter.FilterNode]
  def children: List[RxRouter]
  def isLeaf: Boolean

  def routes: List[RxRoute]

  def wrapWithFilter(parentFilter: RxRouter.FilterNode): RxRouter

  override def toString: String = printNode(0)

  private def printNode(indentLevel: Int): String = {
    val s  = Seq.newBuilder[String]
    val ws = " " * (indentLevel * 2)
    s += s"${ws}- Router[${name}]"

    if (isLeaf) {
      for (r <- routes) {
        val rstr = r.toString
        rstr.split("\n").map { x =>
          s += s"${ws}  + ${x}"
        }
      }
    }

    for (c <- children) {
      s += c.printNode(indentLevel + 1)
    }
    s.result().mkString("\n")
  }
}

object RxRouter extends RxRouterObjectBase {

  def of(endpoint: RxHttpEndpoint): RxRouter = {
    EndpointNode(
      controllerSurface = Surface.of[RedirectToRxEndpoint],
      methodSurfaces = Surface.methodsOf[RedirectToRxEndpoint],
      controllerInstance = Some(endpoint)
    )
  }

  def of(routers: RxRouter*): RxRouter = {
    if (routers.size == 1) {
      routers.head
    } else {
      StemNode(children = routers.toList)
    }
  }

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
    override def wrapWithFilter(parentFilter: FilterNode): RxRouter = {
      this.copy(filter = parentFilter.andThenOpt(filter))
    }

    override def isLeaf: Boolean = false

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
      methodSurfaces: Seq[MethodSurface],
      controllerInstance: Option[Any] = None
  ) extends RxRouter {
    override def name: String               = controllerSurface.name
    override def filter: Option[FilterNode] = None
    override def children: List[RxRouter]   = Nil
    override def isLeaf: Boolean            = true

    override def wrapWithFilter(parentFilter: FilterNode): RxRouter = {
      StemNode(filter = Some(parentFilter), children = List(this))
    }
    override def routes: List[RxRoute] =
      List(RxRoute(None, controllerSurface, methodSurfaces))
  }

  case class RxEndpointNode(endpoint: RxHttpEndpoint) extends RxRouter {
    override def name: String               = f"${this.hashCode()}%08x"
    override def filter: Option[FilterNode] = None
    override def children: List[RxRouter]   = Nil
    override def isLeaf: Boolean            = true
    override def wrapWithFilter(parentFilter: FilterNode): RxRouter = {
      StemNode(filter = Some(parentFilter), children = List(this))
    }

    override def routes: List[RxRoute] = List.empty
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
    def andThen(next: RxRouter*): RxRouter = {
      of(next: _*).wrapWithFilter(this)
    }
  }
}
