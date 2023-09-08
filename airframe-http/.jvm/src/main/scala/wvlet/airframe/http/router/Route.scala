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

import wvlet.airframe.{Session, http}
import wvlet.airframe.codec.{MISSING_PARAMETER, MessageCodecException, MessageCodecFactory}
import wvlet.airframe.http.*
import wvlet.airframe.http.internal.RPCCallContext
import wvlet.airframe.rx.Rx
import wvlet.airframe.surface.{MethodSurface, Surface}
import wvlet.log.LogSupport

import scala.language.higherKinds

/**
  * A mapping from an HTTP endpoint to a corresponding method (or function)
  */
trait Route {
  def httpMethod: String
  def methodSurface: MethodSurface

  def serviceName: String
  def path: String
  val pathComponents: IndexedSeq[String] = {
    path
      .substring(1)
      .split("/")
      .toIndexedSeq
  }

  def controllerSurface: Surface

  def returnTypeSurface: Surface

  /**
    * Returns true if this Route is for `@RPC` call, otherwise returns false (regular `@Endpoint` calls)
    */
  def isRPC: Boolean

  /**
    * Find a corresponding controller and call the matching methods
    */
  def call[Req: HttpRequestAdapter, Resp, F[_]](
      controller: Any,
      request: Req,
      params: Map[String, String],
      context: HttpContext[Req, Resp, F],
      codecFactory: MessageCodecFactory
  ): Any

  private[http] def callWithProvider[Req: HttpRequestAdapter, Resp, F[_]](
      session: Session,
      controllerProvider: ControllerProvider,
      request: Req,
      params: Map[String, String],
      context: HttpContext[Req, Resp, F],
      codecFactory: MessageCodecFactory
  ): Option[Any]
}

/**
  * Define mappings from an HTTP request to a controller method which has the Endpoint annotation
  */
case class ControllerRoute(
    rpcMethod: RPCMethod,
    controllerSurface: Surface,
    httpMethod: String,
    methodSurface: MethodSurface,
    isRPC: Boolean
) extends Route
    with LogSupport {
  require(
    path.startsWith("/"),
    s"Invalid route path: ${path}. EndPoint path must start with a slash (/) in ${methodSurface.owner.name}:${methodSurface.name}"
  )

  def path: String = rpcMethod.path

  override def toString = {
    s"${httpMethod} ${path} -> ${methodSurface.name}(${methodSurface.args
        .map(x => s"${x.name}:${x.surface}").mkString(", ")}): ${methodSurface.returnType}"
  }

  override lazy val serviceName: String = {
    rpcMethod.rpcInterfaceName.replaceAll("\\$anon\\$", "").replaceAll("\\$", ".")
  }

  override def returnTypeSurface: Surface = methodSurface.returnType

  /**
    * Find a corresponding controller and call the matching methods
    */
  override def call[Req: HttpRequestAdapter, Resp, F[_]](
      controller: Any,
      request: Req,
      params: Map[String, String],
      context: HttpContext[Req, Resp, F],
      codecFactory: MessageCodecFactory
  ): Any = {
    controller match {
      case endpoint: RxHttpEndpoint =>
        val adapter = implicitly[HttpRequestAdapter[Req]]
        endpoint.apply(adapter.httpRequestOf(request))
      case _ =>
        var methodArgs: Seq[Any] = Seq.empty
        try {
          try {
            methodArgs = HttpRequestMapper.buildControllerMethodArgs(
              controller,
              methodSurface,
              request,
              context,
              params,
              codecFactory,
              isRPC = isRPC
            )
          } finally {
            // Ensure recording RPC method arguments
            context.setThreadLocal(HttpBackend.TLS_KEY_RPC, RPCCallContext(rpcMethod, methodSurface, methodArgs))
          }
          methodSurface.call(controller, methodArgs: _*)
        } catch {
          case e: IllegalArgumentException =>
            throw RPCStatus.INVALID_REQUEST_U1.newException(s"${request} failed: ${e.getMessage}", e)
          case e: MessageCodecException if e.errorCode == MISSING_PARAMETER =>
            throw RPCStatus.INVALID_REQUEST_U1.newException(e.message, e)
        }
    }
  }

  override private[http] def callWithProvider[Req: HttpRequestAdapter, Resp, F[_]](
      session: Session,
      controllerProvider: ControllerProvider,
      request: Req,
      params: Map[String, String],
      context: HttpContext[Req, Resp, F],
      codecFactory: MessageCodecFactory
  ): Option[Any] = {
    controllerProvider.findController(session, controllerSurface).map { controller =>
      call(controller, request, params, context, codecFactory)
    }
  }
}
