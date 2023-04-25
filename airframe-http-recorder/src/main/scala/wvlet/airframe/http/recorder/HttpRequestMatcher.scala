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
package wvlet.airframe.http.recorder

import wvlet.airframe.http.HttpMessage.{EmptyMessage, Request}
import wvlet.log.LogSupport

import java.util.Locale

/**
  * Compute a hash key of the given HTTP request. This value will be used for DB indexes
  */
trait HttpRequestMatcher {
  def computeHash(request: Request): Int
}

object HttpRequestMatcher extends LogSupport {
  // Http headers to ignore for request hashing purposes
  def defaultExcludeHeaderPrefixes: Seq[String] =
    Seq(
      "date",           // unstable header
      "x-b3-",          // Finagle tracing IDs
      "finagle-",       // Finagle specific headers
      "host",           // The host value can be changed
      "content-length", // this can be 0 (or missing)
      "connection",     // Client might set this header
      "user-agent",     // User-agent can be arbitrary
      "x-http2-",       // Finagle add x-http2- headers
      "pragma",         // Jersey client may add this header
      "cache-control",  // cache-control intention is usually unrelated to specifying the resource
      "http2-settings", // Netty addres HTTP/2 settings
      "upgrade"         // Netty also adds Upgrade request header
    )

  def newRequestMatcher(extraHeadersToExclude: Seq[String]): HttpRequestMatcher = {
    new DefaultHttpRequestMatcher(defaultExcludeHeaderPrefixes ++ extraHeadersToExclude)
  }

  class DefaultHttpRequestMatcher(excludeHeaderPrefixes: Seq[String] = defaultExcludeHeaderPrefixes)
      extends HttpRequestMatcher
      with LogSupport {
    private val excludeHeaderPrefixesLowerCase: Seq[String] = excludeHeaderPrefixes.map(_.toLowerCase(Locale.ENGLISH))

    def computeHash(request: Request): Int = {
      val prefix             = computeRequestPathHash(request)
      val httpHeadersForHash = filterHeaders(request, excludeHeaderPrefixesLowerCase)
      trace(s"http headers for request ${request}: ${httpHeadersForHash.mkString(",")}")

      httpHeadersForHash match {
        case headers if headers.isEmpty => prefix.hashCode * 13
        case headers =>
          val headerHash = headers
            .map { x => s"${x._1.toLowerCase(Locale.ENGLISH)}:${x._2}".hashCode }.reduce { (xor, next) =>
              xor ^ next // Take XOR to compute order-insensitive hash values.
            }
          prefix.hashCode * 13 + headerHash
      }
    }
  }

  private def computeRequestPathHash(request: Request): Int = {
    val contentHash = request.message.contentHash
    val stem        = s"${request.method.toString()}:${request.uri}:${contentHash}"
    stem.hashCode
  }

  def filterHeaders(request: Request, excludePrefixes: Seq[String]): Map[String, String] = {
    request.header.entries
      .filterNot { x =>
        val key = x.key.toLowerCase(Locale.ENGLISH).trim
        excludePrefixes.exists(ex => key.startsWith(ex))
      }
      .map(x => x.key -> x.value)
      .toMap
  }

  object PathOnlyMatcher extends HttpRequestMatcher {
    override def computeHash(request: Request): Int = {
      computeRequestPathHash(request)
    }
  }
}
