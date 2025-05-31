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

import wvlet.airframe.Design
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.HttpMessage.Response
import wvlet.airframe.http.{Http, HttpClientException, HttpStatus}
import wvlet.airspec.AirSpec
import wvlet.log.Logger

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer => JavaHttpServer}
import java.io.{ByteArrayOutputStream, IOException, OutputStream}
import java.net.{InetSocketAddress, URLDecoder}
import java.nio.charset.StandardCharsets
import java.util.zip.{DeflaterOutputStream, GZIPOutputStream}

/**
  */
object URLConnectionClientTest extends AirSpec {

  class MockHttpBinHandler extends HttpHandler {
    override def handle(exchange: HttpExchange): Unit = {
      val method = exchange.getRequestMethod
      val path = exchange.getRequestURI.getPath
      val query = Option(exchange.getRequestURI.getQuery).getOrElse("")
      
      try {
        handleRequest(method, path, query, exchange)
      } catch {
        case _: Exception =>
          exchange.sendResponseHeaders(500, 0)
          exchange.getResponseBody.close()
      }
    }
    
    private def handleRequest(method: String, path: String, query: String, exchange: HttpExchange): Unit = {
      path match {
        case "/get" =>
          val args = parseQuery(query)
          val response = Map(
            "args" -> args,
            "headers" -> Map.empty[String, String],
            "origin" -> "127.0.0.1",
            "url" -> s"http://localhost${exchange.getRequestURI}"
          )
          sendJsonResponse(exchange, 200, MessageCodec.of[Map[String, Any]].toJson(response))
          
        case "/post" | "/put" =>
          val body = readRequestBody(exchange)
          val response = scala.collection.mutable.Map[String, Any](
            "data" -> body,
            "headers" -> Map.empty[String, String],
            "origin" -> "127.0.0.1",
            "url" -> s"http://localhost${exchange.getRequestURI}"
          )
          
          // Try to parse JSON
          if (body.trim.startsWith("{")) {
            try {
              val jsonData = MessageCodec.of[Map[String, Any]].fromJson(body)
              response("json") = jsonData
            } catch {
              case _: Exception => // Not valid JSON, ignore
            }
          }
          
          sendJsonResponse(exchange, 200, MessageCodec.of[Map[String, Any]].toJson(response.toMap))
          
        case "/delete" =>
          val response = Map(
            "args" -> Map.empty[String, String],
            "headers" -> Map.empty[String, String],
            "origin" -> "127.0.0.1",
            "url" -> s"http://localhost${exchange.getRequestURI}"
          )
          sendJsonResponse(exchange, 200, MessageCodec.of[Map[String, Any]].toJson(response))
          
        case "/user-agent" =>
          val userAgent = Option(exchange.getRequestHeaders.getFirst("User-Agent")).getOrElse("")
          val response = Map("user-agent" -> userAgent)
          sendJsonResponse(exchange, 200, MessageCodec.of[Map[String, Any]].toJson(response))
          
        case p if p.startsWith("/status/") =>
          val statusCode = p.substring(8).toInt
          exchange.sendResponseHeaders(statusCode, 0)
          exchange.getResponseBody.close()
          
        case "/gzip" =>
          val data = Map("gzipped" -> true)
          val jsonString = MessageCodec.of[Map[String, Any]].toJson(data)
          val compressed = compressGzip(jsonString.getBytes(StandardCharsets.UTF_8))
          exchange.getResponseHeaders.set("Content-Encoding", "gzip")
          exchange.getResponseHeaders.set("Content-Type", "application/json")
          exchange.sendResponseHeaders(200, compressed.length)
          exchange.getResponseBody.write(compressed)
          exchange.getResponseBody.close()
          
        case "/deflate" =>
          val data = Map("deflated" -> true)
          val jsonString = MessageCodec.of[Map[String, Any]].toJson(data)
          val compressed = compressDeflate(jsonString.getBytes(StandardCharsets.UTF_8))
          exchange.getResponseHeaders.set("Content-Encoding", "deflate")
          exchange.getResponseHeaders.set("Content-Type", "application/json")
          exchange.sendResponseHeaders(200, compressed.length)
          exchange.getResponseBody.write(compressed)
          exchange.getResponseBody.close()
          
        case _ =>
          exchange.sendResponseHeaders(404, 0)
          exchange.getResponseBody.close()
      }
    }
    
    private def sendJsonResponse(exchange: HttpExchange, statusCode: Int, json: String): Unit = {
      val responseBytes = json.getBytes(StandardCharsets.UTF_8)
      exchange.getResponseHeaders.add("Content-Type", "application/json")
      exchange.sendResponseHeaders(statusCode, responseBytes.length)
      val os = exchange.getResponseBody
      os.write(responseBytes)
      os.close()
    }
    
    private def parseQuery(query: String): Map[String, String] = {
      if (query.isEmpty) Map.empty
      else {
        query.split("&").flatMap { param =>
          val parts = param.split("=", 2)
          if (parts.length == 2) {
            Some(URLDecoder.decode(parts(0), StandardCharsets.UTF_8.name()) -> 
                 URLDecoder.decode(parts(1), StandardCharsets.UTF_8.name()))
          } else None
        }.toMap
      }
    }
    
    private def readRequestBody(exchange: HttpExchange): String = {
      val inputStream = exchange.getRequestBody
      val buffer = new Array[Byte](1024)
      val result = new StringBuilder
      var bytesRead = inputStream.read(buffer)
      while (bytesRead != -1) {
        result.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8))
        bytesRead = inputStream.read(buffer)
      }
      result.toString
    }
    
    private def compressGzip(data: Array[Byte]): Array[Byte] = {
      val outputStream = new ByteArrayOutputStream()
      val gzipStream = new GZIPOutputStream(outputStream)
      gzipStream.write(data)
      gzipStream.close()
      outputStream.toByteArray
    }

    private def compressDeflate(data: Array[Byte]): Array[Byte] = {
      val outputStream = new ByteArrayOutputStream()
      val deflateStream = new DeflaterOutputStream(outputStream)
      deflateStream.write(data)
      deflateStream.close()
      outputStream.toByteArray
    }
  }

  private var mockServer: JavaHttpServer = _
  private var serverAddress: String = _
  
  override def beforeAll: Unit = {
    // Start mock server
    mockServer = JavaHttpServer.create(new InetSocketAddress(0), 0)
    mockServer.createContext("/", new MockHttpBinHandler())
    mockServer.setExecutor(null)
    mockServer.start()
    
    val port = mockServer.getAddress.getPort
    serverAddress = s"http://localhost:$port"
  }
  
  override def afterAll: Unit = {
    if (mockServer != null) {
      mockServer.stop(0)
    }
  }

  override protected def design: Design = {
    Design.newDesign
      .bind[SyncClient]
      .toInstance(
        Http.client
          .withBackend(URLConnectionClientBackend)
          .withJSONEncoding
          .newSyncClient(serverAddress)
      )
  }

  case class Person(id: Int, name: String)

  val p     = Person(1, "leo")
  val pJson = MessageCodec.of[Person].toJson(p)

  private def check(r: Response): Unit = {
    r.status shouldBe HttpStatus.Ok_200
    val m = MessageCodec.of[Map[String, Any]].fromJson(r.contentString)
    check(m)
  }

  private def check(m: Map[String, Any]): Unit = {
    m("json") shouldBe Map("id" -> 1, "name" -> "leo")
  }

  test("sync client") { (client: SyncClient) =>
    test("complement missing slash") {
      val resp = client.sendSafe(Http.GET("get"))
      resp.status shouldBe HttpStatus.Ok_200
    }

    test("Read content with 200") {
      val resp = client.sendSafe(Http.GET("/get"))
      debug(resp)
      resp.status shouldBe HttpStatus.Ok_200
      debug(resp.contentString)
    }

    test("user-agent") {
      val resp =
        client
          .withRequestFilter(_.withUserAgent("airframe-http"))
          .sendSafe(Http.GET("/user-agent"))
      MessageCodec.of[Map[String, Any]].fromJson(resp.contentString).get("user-agent") shouldBe Some("airframe-http")
    }

    test("delete") {
      val resp = client.sendSafe(Http.DELETE("/delete"))
      resp.status shouldBe HttpStatus.Ok_200
    }

    test("patch") {
      // URLConnection doesn't support patch, so it internally uses POST endpoint + X-HTTP-Method-Override header
      check(client.call[Person, Map[String, Any]](Http.PATCH("/post"), p))
    }

    test("call with Response return value") {
      check(client.call[Person, Response](Http.POST("/post"), p))
      check(client.call[Person, Response](Http.PUT("/put"), p))
    }

    test("call with GET") {
      val m = client.call[Person, Map[String, Any]](Http.GET(s"/get"), p)
      m("args") shouldBe Map("id" -> "1", "name" -> "leo")
    }

    test("call with POST") {
      check(client.call[Person, Map[String, Any]](Http.POST("/post"), p))
    }

    test("call with PUT") {
      check(client.call[Person, Map[String, Any]](Http.PUT("/put"), p))
    }

    test("Handle 404 (Not Found)") {
      val errorResp = client.sendSafe(Http.GET("/status/404"))
      debug(errorResp)
      errorResp.status shouldBe HttpStatus.NotFound_404
      debug(errorResp.contentString)
    }
  }

  test("retry test") { (client: SyncClient) =>
    test("Handle 5xx retry") {
      Logger("wvlet.airframe.http.HttpClient").suppressWarnings {
        flaky {
          val e = intercept[HttpClientException] {
            client
              .withRetryContext(_.withMaxRetry(1))
              .send(Http.GET("/status/500"))
          }
          e.status shouldBe HttpStatus.InternalServerError_500
        }
      }
    }
  }
}
