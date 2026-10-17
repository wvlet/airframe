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
package wvlet.airframe.http.netty

import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.{Endpoint, Http, HttpMethod, RxRouter, ServerSentEvent}
import wvlet.airframe.Session
import wvlet.airframe.rx.Rx
import wvlet.airspec.AirSpec

import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.util.zip.{Deflater, DeflaterOutputStream, GZIPInputStream, GZIPOutputStream}

object RequestDecompressionTest {
  case class Message(id: Int, text: String)
}

import RequestDecompressionTest.*

class RequestDecompressionApi {
  @Endpoint(method = HttpMethod.POST, path = "/v1/echo")
  def echo(request: Request): Response = {
    Http
      .response()
      .withContent(request.contentBytes)
      .withHeader("X-Content-Encoding", request.header.get("Content-Encoding").getOrElse("none"))
  }

  @Endpoint(method = HttpMethod.POST, path = "/v1/message")
  def message(message: Message): Message = message.copy(text = s"echo: ${message.text}")

  @Endpoint(method = HttpMethod.POST, path = "/v1/sse")
  def sse(request: Request): Rx[ServerSentEvent] = {
    Rx.fromSeq(
      Seq(
        ServerSentEvent(data = s"size=${request.contentBytes.length}"),
        ServerSentEvent(data = new String(request.contentBytes, StandardCharsets.UTF_8))
      )
    )
  }
}

class RequestDecompressionTest extends AirSpec {
  private val router = RxRouter.of[RequestDecompressionApi]

  // A server with the default configuration shared by most tests
  private var session: Session    = null
  private var server: NettyServer = null

  override protected def beforeAll: Unit = {
    session = newServerConfig.design.newSession
    session.start
    server = session.build[NettyServer]
  }

  override protected def afterAll: Unit = {
    session.shutdown
  }
  private val client = HttpClient.newHttpClient()

  private def gzip(data: Array[Byte]): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val gz  = new GZIPOutputStream(out)
    gz.write(data)
    gz.close()
    out.toByteArray
  }

  private def deflate(data: Array[Byte]): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val d   = new DeflaterOutputStream(out, new Deflater())
    d.write(data)
    d.close()
    out.toByteArray
  }

  private def gunzip(data: Array[Byte]): Array[Byte] = {
    val in = new GZIPInputStream(new java.io.ByteArrayInputStream(data))
    try in.readAllBytes()
    finally in.close()
  }

  private def post(
      server: NettyServer,
      path: String,
      body: Array[Byte],
      headers: (String, String)*
  ): HttpResponse[Array[Byte]] = {
    val b = HttpRequest
      .newBuilder(URI.create(s"http://${server.localAddress}${path}"))
      .POST(HttpRequest.BodyPublishers.ofByteArray(body))
    headers.foreach { case (k, v) => b.header(k, v) }
    client.send(b.build(), HttpResponse.BodyHandlers.ofByteArray())
  }

  private def newServerConfig: NettyServerConfig =
    Netty.server.withRouter(router).noLogging.withShutdownQuietPeriod(0)

  private val text: String        = (0 until 1000).map(i => s"line ${i}: hello gzip").mkString("\n")
  private val textBytes           = text.getBytes(StandardCharsets.UTF_8)
  private val binary: Array[Byte] = Array.tabulate[Byte](256 * 64)(i => (i % 256).toByte)

  test("exact raw bytes") {
    val resp = post(server, "/v1/echo", gzip(binary), "Content-Encoding" -> "gzip")
    resp.statusCode() shouldBe 200
    resp.body().toSeq shouldBe binary.toSeq
    // The Content-Encoding header is removed after decoding
    resp.headers().firstValue("X-Content-Encoding").orElse("") shouldBe "none"
  }

  test("x-gzip and deflate") {
    post(server, "/v1/echo", gzip(textBytes), "Content-Encoding" -> "x-gzip").body().toSeq shouldBe textBytes.toSeq
    post(server, "/v1/echo", deflate(textBytes), "Content-Encoding" -> "deflate").body().toSeq shouldBe
      textBytes.toSeq
  }

  test("typed JSON") {
    val json = """{"id":1,"text":"hello"}""".getBytes(StandardCharsets.UTF_8)
    val resp = post(
      server,
      "/v1/message",
      gzip(json),
      "Content-Encoding" -> "gzip",
      "Content-Type"     -> "application/json"
    )
    resp.statusCode() shouldBe 200
    new String(resp.body(), StandardCharsets.UTF_8) shouldBe """{"id":1,"text":"echo: hello"}"""
  }

  test("uncompressed requests") {
    val resp = post(server, "/v1/echo", textBytes)
    resp.statusCode() shouldBe 200
    resp.body().toSeq shouldBe textBytes.toSeq
  }

  test("unsupported encodings are passed through") {
    val resp = post(server, "/v1/echo", textBytes, "Content-Encoding" -> "br")
    resp.statusCode() shouldBe 200
    resp.body().toSeq shouldBe textBytes.toSeq
    resp.headers().firstValue("X-Content-Encoding").orElse("") shouldBe "br"
  }

  test("multiple requests on the same connection") {
    for (i <- 0 until 5) {
      val data = s"request ${i}".getBytes(StandardCharsets.UTF_8)
      val resp =
        if (i % 2 == 0) post(server, "/v1/echo", gzip(data), "Content-Encoding" -> "gzip")
        else post(server, "/v1/echo", data)
      resp.body().toSeq shouldBe data.toSeq
    }
  }

  test("compress responses") {
    val resp = post(server, "/v1/echo", gzip(textBytes), "Content-Encoding" -> "gzip", "Accept-Encoding" -> "gzip")
    resp.statusCode() shouldBe 200
    resp.headers().firstValue("Content-Encoding").orElse("") shouldBe "gzip"
    gunzip(resp.body()).toSeq shouldBe textBytes.toSeq
  }

  test("gzip upload receiving SSE") {
    val resp = post(
      server,
      "/v1/sse",
      gzip(textBytes),
      "Content-Encoding" -> "gzip",
      "Accept"           -> "text/event-stream"
    )
    resp.statusCode() shouldBe 200
    resp.headers().firstValue("Content-Type").orElse("") shouldContain "text/event-stream"
    val body = new String(resp.body(), StandardCharsets.UTF_8)
    body shouldContain s"data: size=${textBytes.length}"
    body shouldContain "data: line 999: hello gzip"
  }

  test("malformed gzip") {
    val resp =
      post(server, "/v1/echo", "not a gzip stream".getBytes(StandardCharsets.UTF_8), "Content-Encoding" -> "gzip")
    resp.statusCode() shouldBe 400
  }

  test("lenient gzip framing follows Netty's decoder") {
    // Netty's gzip decoder does not reject an incomplete stream at the end of the request body, and stops at
    // bytes that do not start a new gzip member. These tests pin the current behavior so that a change
    // in a Netty upgrade will be noticed.
    val gz = gzip(textBytes)

    def decode(data: Array[Byte]): Array[Byte] = {
      val resp = post(server, "/v1/echo", data, "Content-Encoding" -> "gzip")
      resp.statusCode() shouldBe 200
      resp.body()
    }

    test("missing trailer") {
      decode(gz.take(gz.length - 4)).toSeq shouldBe textBytes.toSeq
    }

    test("truncated data yields a partial body") {
      val body = decode(gz.take(gz.length / 2))
      (body.length < textBytes.length) shouldBe true
      textBytes.take(body.length).toSeq shouldBe body.toSeq
    }

    test("trailing junk is ignored") {
      decode(gz ++ "junk".getBytes(StandardCharsets.UTF_8)).toSeq shouldBe textBytes.toSeq
    }

    test("truncated header of a subsequent member is ignored") {
      decode(gz ++ Array[Byte](0x1f.toByte)).toSeq shouldBe textBytes.toSeq
    }

    test("concatenated members") {
      val tail = "tail".getBytes(StandardCharsets.UTF_8)
      decode(gz ++ gzip(tail)).toSeq shouldBe (textBytes ++ tail).toSeq
    }
  }

  test("bound the decoded size of compressed requests") {
    newServerConfig.withMaxDecompressedRequestSize(1024).start { server =>
      // Reject a compressed request exceeding the limit
      {
        val data = new Array[Byte](64 * 1024)
        val resp = post(server, "/v1/echo", gzip(data), "Content-Encoding" -> "gzip")
        resp.statusCode() shouldBe 413
      }

      // Accept a compressed request within the limit
      {
        val data = new Array[Byte](1024)
        val resp = post(server, "/v1/echo", gzip(data), "Content-Encoding" -> "gzip")
        resp.statusCode() shouldBe 200
        resp.body().length shouldBe 1024
      }

      // Uncompressed requests are not limited
      {
        val data = new Array[Byte](64 * 1024)
        val resp = post(server, "/v1/echo", data)
        resp.statusCode() shouldBe 200
        resp.body().length shouldBe data.length
      }
    }
  }

  test("pass through compressed bytes with noRequestDecompression") {
    newServerConfig.noRequestDecompression.start { server =>
      val gz   = gzip(textBytes)
      val resp = post(server, "/v1/echo", gz, "Content-Encoding" -> "gzip")
      resp.statusCode() shouldBe 200
      resp.body().toSeq shouldBe gz.toSeq
      resp.headers().firstValue("X-Content-Encoding").orElse("") shouldBe "gzip"
    }
  }
}
