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

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import wvlet.airframe.codec.MessageCodec

import java.io.ByteArrayOutputStream
import java.net.{InetAddress, InetSocketAddress, URLDecoder}
import java.nio.charset.StandardCharsets
import java.util.concurrent.{Executors, ThreadFactory}
import java.util.zip.{DeflaterOutputStream, GZIPOutputStream}

/**
  * An in-process server implementing the subset of httpbin.org endpoints used by the HTTP client tests. httpbin.org
  * rate-limits echo requests per client IP, which made these tests fail when CI jobs ran in parallel.
  *
  * Built on the JDK HTTP server so that airframe-http tests need no server backend.
  */
object LocalHttpbin {

  /**
    * Base URL of the shared server, started on first use and stopped when the JVM exits
    */
  lazy val address: String = {
    val server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress, 0), 0)
    server.createContext("/", (exchange: HttpExchange) => handle(exchange))
    server.setExecutor(Executors.newCachedThreadPool(new ThreadFactory {
      override def newThread(r: Runnable): Thread = {
        val t = new Thread(r, "local-httpbin")
        t.setDaemon(true)
        t
      }
    }))
    server.start()
    s"http://localhost:${server.getAddress.getPort}/"
  }

  private val mapCodec = MessageCodec.of[Map[String, Any]]
  private val anyCodec = MessageCodec.of[Any]

  private def handle(exchange: HttpExchange): Unit = {
    try {
      val method = exchange.getRequestMethod
      val path   = exchange.getRequestURI.getPath
      val body   = readBody(exchange)
      path match {
        case "/get" | "/post" | "/put" | "/patch" | "/delete" if path.substring(1).equalsIgnoreCase(method) =>
          respondJson(exchange, 200, echo(exchange, body))
        case "/user-agent" =>
          respondJson(
            exchange,
            200,
            Map("user-agent" -> Option(exchange.getRequestHeaders.getFirst("User-Agent")).getOrElse(""))
          )
        case p if p.startsWith("/status/") =>
          respond(exchange, p.stripPrefix("/status/").toInt, Array.emptyByteArray, None)
        case "/gzip" =>
          respondJson(exchange, 200, Map("gzipped" -> true), Some("gzip"))
        case "/deflate" =>
          respondJson(exchange, 200, Map("deflated" -> true), Some("deflate"))
        case _ =>
          respond(exchange, 404, Array.emptyByteArray, None)
      }
    } finally {
      exchange.close()
    }
  }

  private def echo(exchange: HttpExchange, body: String): Map[String, Any] = {
    val headers = Map.newBuilder[String, String]
    val it      = exchange.getRequestHeaders.entrySet().iterator()
    while (it.hasNext) {
      val e = it.next()
      headers += canonicalHeaderName(e.getKey) -> String.join(",", e.getValue)
    }
    Map(
      "args"    -> queryParams(exchange.getRequestURI.getRawQuery),
      "data"    -> body,
      "headers" -> headers.result(),
      "json"    -> (if (body.isEmpty) null else anyCodec.fromJson(body)),
      "url"     -> s"http://localhost${exchange.getRequestURI}"
    )
  }

  // The JDK server normalizes header names to "Content-type"; httpbin returns "Content-Type"
  private def canonicalHeaderName(name: String): String = {
    name.split("-").map(_.toLowerCase.capitalize).mkString("-")
  }

  private def queryParams(rawQuery: String): Map[String, String] = {
    Option(rawQuery).toSeq
      .flatMap(_.split("&"))
      .filter(_.nonEmpty)
      .map { kv =>
        def decode(s: String) = URLDecoder.decode(s, "UTF-8")
        kv.split("=", 2) match {
          case Array(k, v) => decode(k) -> decode(v)
          case Array(k)    => decode(k) -> ""
        }
      }
      .toMap
  }

  private def readBody(exchange: HttpExchange): String = {
    val in  = exchange.getRequestBody
    val out = new ByteArrayOutputStream()
    val buf = new Array[Byte](8192)
    var n   = in.read(buf)
    while (n != -1) {
      out.write(buf, 0, n)
      n = in.read(buf)
    }
    new String(out.toByteArray, StandardCharsets.UTF_8)
  }

  private def respondJson(
      exchange: HttpExchange,
      status: Int,
      json: Map[String, Any],
      contentEncoding: Option[String] = None
  ): Unit = {
    exchange.getResponseHeaders.set("Content-Type", "application/json")
    val bytes = mapCodec.toJson(json).getBytes(StandardCharsets.UTF_8)
    respond(exchange, status, bytes, contentEncoding)
  }

  private def respond(exchange: HttpExchange, status: Int, body: Array[Byte], contentEncoding: Option[String]): Unit = {
    val encoded = contentEncoding match {
      case Some(encoding) =>
        exchange.getResponseHeaders.set("Content-Encoding", encoding)
        val out = new ByteArrayOutputStream()
        val enc = if (encoding == "gzip") new GZIPOutputStream(out) else new DeflaterOutputStream(out)
        enc.write(body)
        enc.close()
        out.toByteArray
      case None =>
        body
    }
    // -1 tells the JDK server to send no response body
    exchange.sendResponseHeaders(status, if (encoded.isEmpty) -1 else encoded.length.toLong)
    if (encoded.nonEmpty) {
      exchange.getResponseBody.write(encoded)
    }
  }
}
