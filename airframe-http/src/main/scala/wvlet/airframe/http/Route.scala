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
import wvlet.airframe.surface.Surface
import wvlet.airframe.surface.reflect.ReflectMethodSurface
import wvlet.log.LogSupport

/**
  * Define mappings from an HTTP request to a controller method which has the Endpoint annotation
  */
case class Route(controllerSurface: Surface, method: HttpMethod, path: String, methodSurface: ReflectMethodSurface)
    extends LogSupport {
  require(
    path.startsWith("/"),
    s"Invalid route path: ${path}. EndPoint path must start with a slash (/) in ${methodSurface.owner.name}:${methodSurface.name}"
  )

  override def toString =
    s"${method} ${path} -> ${methodSurface.name}(${methodSurface.args
      .map(x => s"${x.name}:${x.surface}").mkString(", ")}): ${methodSurface.returnType}"

  val pathComponents: IndexedSeq[String] = {
    path
      .substring(1)
      .split("/")
      .toIndexedSeq
  }

  def returnTypeSurface: Surface = methodSurface.returnType

  /**
    * Find a corresponding controller and call the matching methods
    */
  def call[Req: HttpRequestAdapter](
      controller: Any,
      request: Req,
      params: Map[String, String]
  ): Any = {
    val methodArgs = HttpRequestMapper.buildControllerMethodArgs(controller, methodSurface, request, params)
    methodSurface.call(controller, methodArgs: _*)
  }

  private[http] def call[Req: HttpRequestAdapter](
      controllerProvider: ControllerProvider,
      request: Req,
      params: Map[String, String]
  ): Option[Any] = {
    controllerProvider.findController(controllerSurface).map { controller =>
      call(controller, request, params)
    }
  }
}
