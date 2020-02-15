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

import java.util.Locale

import com.twitter.finagle.http.{MediaType, Request}
import wvlet.log.LogSupport

/**
  * Compute a hash key of the given HTTP request.
  * This value will be used for DB indexes
  */
trait HttpRequestMatcher {
  def computeHash(request: Request): Int
}

object HttpRequestMatcher {
  // Http headers to ignore for request hashing purposes
  def defaultExcludeHeaderPrefixes: Seq[String] = Seq(
    "date",           // unstable header
    "x-b3-",          // Finagle's tracing IDs
    "finagle-",       // Finagle specific headers
    "host",           // The host value can be changed
    "content-length", // this can be 0 (or missing)
    "connection",     // Client might set this header
    "user-agent"      // User-agent can be arbitrary
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
    val contentHash = request.content.hashCode()
    s"${request.method.toString()}:${request.uri}:${contentHash}".hashCode
  }

  def filterHeaders(request: Request, excludePrefixes: Seq[String]): Map[String, String] = {
    request.headerMap.toSeq.filterNot { x =>
      val key = x._1.toLowerCase(Locale.ENGLISH)
      excludePrefixes.exists(ex => key.startsWith(ex))
    }.toMap
  }

  object PathOnlyMatcher extends HttpRequestMatcher {
    override def computeHash(request: Request): Int = {
      computeRequestPathHash(request)
    }
  }
}
