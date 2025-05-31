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
import wvlet.airframe.http.*
import wvlet.airframe.rx.Rx

/**
 * Mock HTTP server API that simulates httpbin.org and jsonplaceholder.typicode.com endpoints
 * for testing HTTP clients without relying on external services.
 */
class MockServer {

  @Endpoint(method = HttpMethod.GET, path = "/get")
  def get(request: HttpMessage.Request): HttpMessage.Response = {
    val queryParams = parseQueryParams(request.uri)
    val response = Map(
      "args" -> queryParams,
      "headers" -> request.header.entries.map(e => e.key -> e.value).toMap,
      "origin" -> "127.0.0.1",
      "url" -> s"http://localhost${request.uri}"
    )
    Http.response(HttpStatus.Ok_200).withJson(MessageCodec.of[Map[String, Any]].toJson(response))
  }

  @Endpoint(method = HttpMethod.POST, path = "/post")
  def post(request: HttpMessage.Request): HttpMessage.Response = {
    val response = Map(
      "args" -> Map.empty[String, String],
      "data" -> request.contentString,
      "files" -> Map.empty[String, String],
      "form" -> Map.empty[String, String],
      "headers" -> request.header.entries.map(e => e.key -> e.value).toMap,
      "json" -> parseJsonOrNull(request.contentString),
      "origin" -> "127.0.0.1",
      "url" -> s"http://localhost${request.uri}"
    )
    Http.response(HttpStatus.Ok_200).withJson(MessageCodec.of[Map[String, Any]].toJson(response))
  }

  @Endpoint(method = HttpMethod.PUT, path = "/put")
  def put(request: HttpMessage.Request): HttpMessage.Response = {
    val response = Map(
      "args" -> Map.empty[String, String],
      "data" -> request.contentString,
      "files" -> Map.empty[String, String],
      "form" -> Map.empty[String, String],
      "headers" -> request.header.entries.map(e => e.key -> e.value).toMap,
      "json" -> parseJsonOrNull(request.contentString),
      "origin" -> "127.0.0.1",
      "url" -> s"http://localhost${request.uri}"
    )
    Http.response(HttpStatus.Ok_200).withJson(MessageCodec.of[Map[String, Any]].toJson(response))
  }

  @Endpoint(method = HttpMethod.DELETE, path = "/delete")
  def delete(request: HttpMessage.Request): HttpMessage.Response = {
    val response = Map(
      "args" -> Map.empty[String, String],
      "data" -> "",
      "files" -> Map.empty[String, String],
      "form" -> Map.empty[String, String],
      "headers" -> request.header.entries.map(e => e.key -> e.value).toMap,
      "json" -> null,
      "origin" -> "127.0.0.1",
      "url" -> s"http://localhost${request.uri}"
    )
    Http.response(HttpStatus.Ok_200).withJson(MessageCodec.of[Map[String, Any]].toJson(response))
  }

  @Endpoint(method = HttpMethod.GET, path = "/user-agent")
  def userAgent(request: HttpMessage.Request): HttpMessage.Response = {
    val userAgent = request.header.get("User-Agent").getOrElse("")
    val response = Map("user-agent" -> userAgent)
    Http.response(HttpStatus.Ok_200).withJson(MessageCodec.of[Map[String, Any]].toJson(response))
  }

  @Endpoint(method = HttpMethod.GET, path = "/status/:code")
  def status(code: Int): HttpMessage.Response = {
    val status = HttpStatus.ofCode(code)
    Http.response(status)
  }

  @Endpoint(method = HttpMethod.GET, path = "/gzip")
  def gzip(): HttpMessage.Response = {
    val response = Map(
      "gzipped" -> true,
      "headers" -> Map("Accept-Encoding" -> "gzip, deflate"),
      "origin" -> "127.0.0.1"
    )
    Http.response(HttpStatus.Ok_200)
      .withJson(MessageCodec.of[Map[String, Any]].toJson(response))
      .withHeader("Content-Encoding", "gzip")
  }

  @Endpoint(method = HttpMethod.GET, path = "/deflate")
  def deflate(): HttpMessage.Response = {
    val response = Map(
      "deflated" -> true,
      "headers" -> Map("Accept-Encoding" -> "gzip, deflate"),
      "origin" -> "127.0.0.1"
    )
    Http.response(HttpStatus.Ok_200)
      .withJson(MessageCodec.of[Map[String, Any]].toJson(response))
      .withHeader("Content-Encoding", "deflate")
  }

  // JSONPlaceholder.typicode.com endpoints
  @Endpoint(method = HttpMethod.GET, path = "/posts/:id")
  def getPost(id: Int): HttpMessage.Response = {
    val post = Map(
      "userId" -> 1,
      "id" -> id,
      "title" -> s"Sample post ${id}",
      "body" -> s"Sample body for post ${id}"
    )
    Http.response(HttpStatus.Ok_200).withJson(MessageCodec.of[Map[String, Any]].toJson(post))
  }

  @Endpoint(method = HttpMethod.POST, path = "/posts")
  def createPost(request: HttpMessage.Request): HttpMessage.Response = {
    val response = Map(
      "id" -> 101
    )
    Http.response(HttpStatus.Created_201).withJson(MessageCodec.of[Map[String, Any]].toJson(response))
  }

  private def parseQueryParams(uri: String): Map[String, String] = {
    val queryStart = uri.indexOf('?')
    if (queryStart == -1) {
      Map.empty
    } else {
      val query = uri.substring(queryStart + 1)
      query.split("&").map { param =>
        val eq = param.indexOf('=')
        if (eq == -1) {
          param -> ""
        } else {
          param.substring(0, eq) -> param.substring(eq + 1)
        }
      }.toMap
    }
  }

  private def parseJsonOrNull(content: String): Any = {
    try {
      if (content.nonEmpty && content.trim.startsWith("{")) {
        MessageCodec.of[Map[String, Any]].fromJson(content)
      } else {
        null
      }
    } catch {
      case _: Exception => null
    }
  }
}