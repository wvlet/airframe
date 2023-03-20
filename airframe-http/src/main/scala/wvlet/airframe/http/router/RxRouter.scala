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
import wvlet.airframe.surface.TypeName.sanitizeTypeName
import wvlet.airframe.surface.{MethodSurface, Surface}

case class RxFilterDef(filter: RxFilter, filterSurface: Surface) {
  def name: String = sanitizeTypeName(filterSurface.rawType.getClass.getSimpleName)
}

sealed trait RxRouter {
  def name: String
  def parent: Option[RxRouter]
  def children: Seq[RxRouter]

  def isLeaf: Boolean
  def isNode: Boolean

  def withParent(parent: RxRouter): RxRouter

  def isRoot: Boolean = parent.isEmpty

  def root: RxRouter = {
    parent.map(_.root).getOrElse(this)
  }
}

//  /**
//    * Chain a router and return the context router
//    * @param next
//    * @return
//    */
//  def andThen(next: RxRouter): RxRouter = {
//    siblings.size match {
//      case s if s <= 1 =>
//        next.withParent(this)
//      case other =>
//        throw new IllegalStateException(
//          s"Cannot add child router ${next.name} to ${this.name} if it already has multiple siblings"
//        )
//    }
//  }
//
//  def add(router: RxRouter): RxRouter = {
//    this.copy(siblings = siblings :+ router.withParent(this))
//  }
//
//  def addInternal(controllerSurface: Surface, methodSurfaces: Seq[MethodSurface]): RxRouter = {
//    add(RxRouter(None, Some(controllerSurface), methodSurfaces))
//  }
//}

object RxRouter extends RxRouterObjectBase {

  def empty: RxRouter = EmptyNode

  case object EmptyNode extends RxRouter {
    override def name: String = "empty"
    override def withParent(parent: RxRouter): RxRouter = throw new UnsupportedOperationException(
      "Adding parent to empty node is not supported"
    )
    override def parent: Option[RxRouter] = None
    override def isLeaf: Boolean          = true
    override def isNode: Boolean          = false

    override def children: Seq[RxRouter] = Seq.empty
  }

  case class RxFilterNode(
      override val parent: Option[RxRouter],
      filterDef: RxFilterDef
  ) extends RxRouter {

    override def name: String = {
      filterDef.name
    }

    override def withParent(parent: RxRouter): RxFilterNode = {
      this.copy(parent = Some(parent))
    }
    override def isLeaf: Boolean = false
    override def isNode: Boolean = true

    override def children: Seq[RxRouter] = Seq.empty

    def andThen(next: RxRouter): RxRouter = {
      next.withParent(this)
    }

  }

  case class RxRouterLeaf(
      override val parent: Option[RxRouter],
      controllerSurface: Surface,
      methodSurfaces: Seq[MethodSurface]
  ) extends RxRouter {
    override def name: String    = controllerSurface.name
    override def isLeaf: Boolean = true
    override def isNode: Boolean = false

    override def children: Seq[RxRouter] = Seq.empty

    override def withParent(parent: RxRouter): RxRouter = this.copy(parent = Some(parent))
  }

//  def merge(routers: RxRouter*): RxRouter = {
//    routers.toSeq.reduce { (r1, r2) => r1.add(r2) }
//  }
}
