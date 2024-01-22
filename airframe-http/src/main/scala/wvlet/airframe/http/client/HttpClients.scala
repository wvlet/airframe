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
package wvlet.airframe.http.client

import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.codec.PrimitiveCodec.UnitCodec
import wvlet.airframe.control.Retry.{MaxRetryException, RetryContext}
import wvlet.airframe.control.{CircuitBreakerOpenException, Retry}
import wvlet.airframe.http.*
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.util.Try
import scala.util.control.NonFatal

/**
  * Implements a common logic for HTTP clients, such as retry patterns, error handling, RPC response handling, etc.
  */
object HttpClients extends LogSupport {
  import wvlet.airframe.http.internal.HttpResponseBodyCodec
  import wvlet.airframe.http.{HttpRequestAdapter, HttpResponseAdapter}
  import wvlet.airframe.json.JSON.{JSONArray, JSONObject}

  import scala.reflect.ClassTag

  private val responseBodyCodec = new HttpResponseBodyCodec[Response]

  private[http] def baseHttpClientRetry[Req: HttpRequestAdapter: ClassTag, Resp: HttpResponseAdapter]: RetryContext = {
    Retry
      .withJitter(maxRetry = 15)
      .withResultClassifier(HttpClientException.classifyHttpResponse[Resp])
      .beforeRetry(defaultBeforeRetryAction[Req])
  }

  def defaultHttpClientRetry[Req: HttpRequestAdapter: ClassTag, Resp: HttpResponseAdapter]: RetryContext = {
    baseHttpClientRetry[Req, Resp]
      // JVM specific error handler
      .withErrorClassifier(HttpClientException.classifyExecutionFailure)
  }

  def defaultBeforeRetryAction[Req: HttpRequestAdapter: ClassTag](ctx: RetryContext): Any = {
    val cls = implicitly[ClassTag[Req]].runtimeClass

    val errorMessage = ctx.context match {
      case Some(r) if cls.isAssignableFrom(r.getClass) =>
        val adapter = implicitly[HttpRequestAdapter[Req]]
        val req     = r.asInstanceOf[Req]
        val path    = adapter.pathOf(req)
        s"Request to ${path} is failed: ${ctx.lastError.getMessage}"
      case _ =>
        s"Request is failed: ${ctx.lastError.getMessage}"
    }

    val nextWaitMillis = ctx.nextWaitMillis
    warn(
      f"[${ctx.retryCount}/${ctx.maxRetry}] ${errorMessage}. Retry the request in ${nextWaitMillis / 1000.0}%.3f sec."
    )
  }

  private[client] def defaultHttpClientErrorHandler(
      // Need to evaluate the last response lazily because it may not be available when this method is called
      lastResponse: () => Option[Response]
  ): PartialFunction[Throwable, Nothing] = {
    case e: HttpClientException =>
      val resp = e.response.toHttpResponse
      resp.getHeader(HttpHeader.xAirframeRPCStatus) match {
        case Some(status) =>
          // Throw RPCException if RPCStatus code is given
          val ex = RPCException.fromResponse(e.response.toHttpResponse)
          throw new HttpClientException(resp, ex.status.httpStatus, ex.message, ex)
        case None =>
          // Throw as is for known client exception
          throw e
      }
    case e: RPCException =>
      // This path is used only for local RPC client tests, which do not launch any real HTTP server
      val resp = e.toResponse
      throw new HttpClientException(resp, e.status.httpStatus, e.message, e)
    case e: CircuitBreakerOpenException =>
      val resp = lastResponse().getOrElse(Http.response(HttpStatus.ServiceUnavailable_503))
      throw new HttpClientException(
        resp,
        status = resp.status,
        message = e.getMessage,
        cause = e
      )
    case e: MaxRetryException =>
      throw HttpClientMaxRetryException(
        lastResponse().getOrElse(Http.response(HttpStatus.InternalServerError_500)),
        e.retryContext,
        e.retryContext.lastError
      )
    case NonFatal(e) =>
      val resp = lastResponse().getOrElse(Http.response(HttpStatus.InternalServerError_500))
      throw new HttpClientException(
        resp,
        status = resp.status,
        message = e.getMessage,
        cause = e
      )
  }

  def urlEncode(s: String): String = {
    compat.urlEncode(s)
  }

  /**
    * Generate a GET resource url by embedding the resource object into query parameters
    *
    * @param path
    * @param resource
    * @param resourceSurface
    * @tparam Resource
    * @return
    */
  private[http] def buildResourceUri[Resource](path: String, resource: Resource, resourceSurface: Surface): String = {
    val queryParams    = flattenResourceToQueryParams(resource, resourceSurface)
    val pathWithParams = new StringBuilder()
    pathWithParams.append(path)
    if (queryParams.nonEmpty) {
      val queryParamString = queryParams.entries.map(x => s"${x.key}=${x.value}").mkString("&")
      pathWithParams.append("?")
      pathWithParams.append(queryParamString)
    }
    pathWithParams.result()
  }

  /**
    * Flatten resource objects into query parameters for GET request
    *
    * @param resource
    * @param resourceSurface
    * @tparam Resource
    * @return
    */
  private[http] def flattenResourceToQueryParams[Resource](
      resource: Resource,
      resourceSurface: Surface
  ): HttpMultiMap = {
    val resourceCodec = MessageCodec.ofSurface(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    val resourceJson  = Try(resourceCodec.toJSONObject(resource)).getOrElse(JSONObject.empty)

    val queryParams = HttpMultiMap.newBuilder
    resourceJson.v.map {
      case (k, j @ JSONArray(_)) =>
        queryParams += urlEncode(k) -> urlEncode(j.toJSON) // Flatten the JSON array value
      case (k, j @ JSONObject(_)) =>
        queryParams += urlEncode(k) -> urlEncode(j.toJSON) // Flatten the JSON object value
      case (k, other) =>
        queryParams += urlEncode(k) -> urlEncode(other.toString)
    }
    queryParams.result()
  }

  private[client] def prepareRequest[Req](
      config: HttpClientConfig,
      baseRequest: Request,
      requestSurface: Surface,
      requestBody: Req
  ): Request = {
    try {
      baseRequest.method match {
        case HttpMethod.GET =>
          val newPath = buildResourceUri[Req](baseRequest.path, requestBody, requestSurface)
          baseRequest.withUri(newPath)
        case _ =>
          val requestCodec: MessageCodec[Req] =
            config.codecFactory.ofSurface(requestSurface).asInstanceOf[MessageCodec[Req]]
          val bytes = config.rpcEncoding.encodeWithCodec(requestBody, requestCodec)
          config.rpcEncoding match {
            case RPCEncoding.MsgPack =>
              baseRequest.withMsgPack(bytes)
            case RPCEncoding.JSON =>
              baseRequest.withJson(bytes)
          }
      }
    } catch {
      case e: Throwable =>
        throw new HttpClientException(
          Http.response(HttpStatus.BadRequest_400),
          HttpStatus.BadRequest_400,
          s"Failed to encode the HTTP request body: ${requestBody}",
          e
        )
    }
  }

  private[client] def parseResponse[Resp](
      config: HttpClientConfig,
      responseSurface: Surface,
      resp: Response
  ): Resp = {
    // If the response type is Response, return as is
    if (classOf[Response].isAssignableFrom(responseSurface.rawType)) {
      resp.asInstanceOf[Resp]
    } else {
      try {
        val msgpack = responseBodyCodec.toMsgPack(resp)
        val codec   = config.codecFactory.ofSurface(responseSurface)
        codec match {
          case UnitCodec =>
            null.asInstanceOf[Resp]
          case _ =>
            val responseObject = codec.fromMsgPack(msgpack)
            responseObject.asInstanceOf[Resp]
        }
      } catch {
        case e: Throwable =>
          throw new HttpClientException(
            resp,
            resp.status,
            s"Failed to parse the response from the server: ${resp}: ${e.getMessage}",
            e
          )
      }
    }
  }

  private[http] def prepareRPCRequest(
      config: HttpClientConfig,
      resourcePath: String,
      requestSurface: Surface,
      requestContent: Any
  ): Request = {
    val requestEncoder: MessageCodec[Any] =
      config.codecFactory.ofSurface(requestSurface).asInstanceOf[MessageCodec[Any]]

    try {
      Http
        .POST(resourcePath)
        .withContentType(config.rpcEncoding.applicationType)
        .withAccept(config.rpcEncoding.applicationType)
        // Encode request body
        .withContent(config.rpcEncoding.encodeWithCodec[Any](requestContent, requestEncoder))
    } catch {
      case e: Throwable =>
        throw RPCStatus.INVALID_ARGUMENT_U2.newException(
          message = s"Failed to encode the RPC request argument ${requestContent}: ${e.getMessage}",
          cause = e
        )
    }
  }

  private[airframe] def parseRPCResponse(
      config: HttpClientConfig,
      response: Response,
      responseSurface: Surface
  ): Any = {
    if (classOf[Response].isAssignableFrom(responseSurface.rawType)) {
      response
    } else {
      try {
        val msgpack = responseBodyCodec.toMsgPack(response)
        val codec   = config.codecFactory.ofSurface(responseSurface)
        codec match {
          case UnitCodec =>
            null
          case _ =>
            val responseObject = codec.fromMsgPack(msgpack)
            responseObject
        }
      } catch {
        case e: Throwable =>
          throw RPCStatus.DATA_LOSS_I8.newException(
            s"Failed to parse the RPC response from the server ${response}: ${e.getMessage}",
            e
          )
      }
    }
  }

}
