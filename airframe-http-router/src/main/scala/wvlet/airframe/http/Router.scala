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

import wvlet.airframe.http.router._
import wvlet.airframe.surface.{GenericSurface, HigherKindedTypeSurface, MethodSurface, Surface}
import wvlet.log.LogSupport

import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.language.higherKinds

/**
  * Router defines mappings from HTTP requests to Routes.
  *
  * Router can be nested
  *   - Router1 with Filter1
  *     - Router2: endpoints e1, e2
  *     - Router3: endpoints e3 with Filter2
  *   - Router4: endpoints e4
  *
  * From this router definition, the backend HTTP server specific implementation will build a mapping table like this:
  * e1 -> Filter1 andThen process(e1) e2 -> Filter1 andThen process(e2) e3 -> Filter1 andThen Filter2 andThen
  * process(e3) e4 -> process(e4)
  */
case class Router(
    surface: Option[Surface] = None,
    children: Seq[Router] = Seq.empty,
    localRoutes: Seq[Route] = Seq.empty,
    filterSurface: Option[Surface] = None,
    filterInstance: Option[HttpFilterType] = None
) extends router.RouterBase
    with LogSupport {
  def isEmpty = this eq Router.empty

  def isLeafFilter = children.isEmpty && localRoutes.isEmpty

  // If this node has no operation (endspoints, filter, etc.)
  def hasNoOperation =
    surface.isEmpty && filterSurface.isEmpty && localRoutes.isEmpty && filterInstance.isEmpty

  def routes: Seq[Route] = {
    localRoutes ++ children.flatMap(_.routes)
  }

  override def toString: String = printNode(0)

  private def routerName: String = {
    surface
      .orElse(filterSurface)
      .orElse(filterInstance.map(_.getClass.getSimpleName))
      .getOrElse(f"${hashCode()}%x")
      .toString
  }

  private def printNode(indentLevel: Int): String = {
    val s = Seq.newBuilder[String]

    val ws = " " * (indentLevel * 2)
    s += s"${ws}- Router[${routerName}]"

    for (r <- localRoutes) {
      s += s"${ws}  + ${r}"
    }
    for (c <- children) {
      s += c.printNode(indentLevel + 1)
    }
    s.result().mkString("\n")
  }

  /**
    * A request filter that will be applied before routing the request to the target method
    */
  private lazy val routeMatcher = RouteMatcher.build(routes)
  def findRoute[Req: HttpRequestAdapter](request: Req): Option[RouteMatch] =
    routeMatcher.findRoute(request)

  /**
    * Call this method to verify duplicated routes in an early phase
    */
  def verifyRoutes: Unit = {
    // Instantiate the route mappings to check duplicate routes
    routeMatcher
  }

  def andThen(filter: HttpFilterType): Router =
    andThen(Router(filterInstance = Some(filter)))

  def andThen(next: Router): Router = {
    this.children.size match {
      case 0 =>
        this.addChild(next)
      case 1 =>
        this.copy(children = Seq(children(0).andThen(next)))
      case _ =>
        throw new IllegalStateException(s"The router ${this.toString} already has multiple child routers")
    }
  }

  /**
    * Add a child and and return a new Router with this child node
    *
    * @param childRouter
    * @return
    */
  def addChild(childRouter: Router): Router = {
    this.copy(children = children :+ childRouter)
  }

  def withFilter(newFilterSurface: Surface): Router = {
    this.copy(filterSurface = Some(newFilterSurface))
  }

  /**
    * Internal only method for adding the surface of the controller
    */
  def addInternal(controllerSurface: Surface, controllerMethodSurfaces: Seq[MethodSurface]): Router = {
    // Import ReflectSurface to find method annotations (Endpoint)
    import wvlet.airframe.surface.reflect._

    val endpointOpt = controllerSurface.findAnnotationOf[Endpoint]
    val rpcOpt      = controllerSurface.findAnnotationOf[RPC]

    val newRoutes: Seq[ControllerRoute] = {
      (endpointOpt, rpcOpt) match {
        case (Some(endpoint), Some(rpcOpt)) =>
          throw new IllegalArgumentException(
            s"Cannot define both of @Endpoint and @RPC annotations: ${controllerSurface}"
          )
        case (_, None) =>
          val prefixPath = endpointOpt.map(_.path()).getOrElse("")
          // Add methods annotated with @Endpoint
          controllerMethodSurfaces
            .map { m =>
              (m, m.findAnnotationOf[Endpoint])
            }
            .collect { case (m: MethodSurface, Some(endPoint)) =>
              val endpointInterfaceCls =
                controllerSurface
                  .findAnnotationOwnerOf[Endpoint]
                  .getOrElse(controllerSurface.rawType)
              ControllerRoute(
                endpointInterfaceCls,
                controllerSurface,
                endPoint.method(),
                prefixPath + endPoint.path(),
                m,
                isRPC = false
              )
            }
        case (None, Some(rpc)) =>
          val rpcInterfaceCls = Router.findRPCInterfaceCls(controllerSurface)

          def sanitize(s: String): String = {
            s.replaceAll("\\$anon\\$", "").replaceAll("\\$", ".")
          }
          val prefixPath = if (rpc.path().isEmpty) {
            s"/${sanitize(rpcInterfaceCls.getName)}"
          } else {
            s"${rpc.path()}/${sanitize(rpcInterfaceCls.getSimpleName)}"
          }
          val routes = controllerMethodSurfaces
            .filter(_.isPublic)
            .map { m =>
              (m, m.findAnnotationOf[RPC])
            }
            .collect {
              case (m: MethodSurface, Some(rpc)) =>
                val methodPath =
                  if (rpc.path().nonEmpty) rpc.path() else s"/${m.name}"
                ControllerRoute(
                  rpcInterfaceCls,
                  controllerSurface,
                  HttpMethod.POST,
                  prefixPath + methodPath,
                  m,
                  isRPC = true
                )
              case (m: MethodSurface, None) =>
                ControllerRoute(
                  rpcInterfaceCls,
                  controllerSurface,
                  HttpMethod.POST,
                  prefixPath + s"/${m.name}",
                  m,
                  isRPC = true
                )
            }
          routes
      }
    }

    val newRouter =
      new Router(surface = Some(controllerSurface), localRoutes = newRoutes)
    if (this.isEmpty) {
      newRouter
    } else {
      Router.merge(List(this, newRouter))
    }
  }
}

object Router extends RouterObjectBase with LogSupport {
  val empty: Router   = new Router()
  def apply(): Router = empty

  def apply(children: Router*): Router = {
    if (children == null) {
      empty
    } else {
      children.toList match {
        case c :: Nil =>
          c
        case lst =>
          merge(lst)
      }
    }
  }

  def merge(routes: List[Router]): Router = {
    @tailrec
    def loop(h: Router, t: List[Router]): Router = {
      if (t.isEmpty) {
        h
      } else {
        if (h.hasNoOperation) {
          loop(h.addChild(t.head), t.tail)
        } else {
          loop(empty.addChild(h), t)
        }
      }
    }

    loop(routes.head, routes.tail)
  }

  def add(filter: HttpFilterType) = {
    new Router(filterInstance = Some(filter))
  }

  private[http] def isHttpResponse(s: Surface): Boolean = {
    s match {
      case r: GenericSurface if r.rawType == classOf[HttpMessage.Response] =>
        true
      case r: GenericSurface if r.fullName == "com.twitter.finagle.http.Response" =>
        true
      case other =>
        false
    }
  }

  private[http] def isFinagleReader(s: Surface): Boolean = {
    s match {
      case s: Surface if s.fullName.startsWith("com.twitter.io.Reader[") =>
        true
      case other =>
        false
    }
  }

  private[http] def isFuture(s: Surface): Boolean = {
    s match {
      case h: HigherKindedTypeSurface
          if h.typeArgs.size == 1 && h.name == "F" => // Only support 'F' for tagless-final pattern
        true
      case s: Surface
          if s.rawType == classOf[scala.concurrent.Future[_]] || s.rawType.getName == "com.twitter.util.Future" =>
        true
      case _ =>
        false
    }
  }

  private[http] def unwrapFuture(s: Surface): Surface = {
    s match {
      case h: HigherKindedTypeSurface
          if h.typeArgs.size == 1 && h.name == "F" => // Only support 'F' for tagless-final pattern
        h.typeArgs.head
      case s: Surface
          if s.rawType == classOf[scala.concurrent.Future[_]] || s.rawType.getName == "com.twitter.util.Future" =>
        s.typeArgs.head
      case _ =>
        s
    }
  }

  private[http] def findRPCInterfaceCls(controllerSurface: Surface): Class[_] = {
    // Import ReflectSurface to find RPC annotation
    import wvlet.airframe.surface.reflect._

    // We need to find the owner class of the RPC interface because the controller might be extending the RPC interface (e.g., RPCImpl)
    val rpcInterfaceCls = controllerSurface
      .findAnnotationOwnerOf[RPC]
      .getOrElse(controllerSurface.rawType)

    rpcInterfaceCls
  }
}
