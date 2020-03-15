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

object HttpHeader {
  val empty = HttpHeader(Map.empty)

  case class HeaderEntry(key: String, value: String) {
    override def toString: String = s"${key}: ${value}"
  }

  final val Accept                        = "Accept"
  final val AcceptCharset                 = "Accept-Charset"
  final val AcceptEncoding                = "Accept-Encoding"
  final val AcceptLanguage                = "Accept-Language"
  final val AcceptRanges                  = "Accept-Ranges"
  final val AcceptPatch                   = "Accept-Patch"
  final val AccessControlAllowCredentials = "Access-Control-Allow-Credentials"
  final val AccessControlAllowHeaders     = "Access-Control-Allow-Headers"
  final val AccessControlAllowMethods     = "Access-Control-Allow-Methods"
  final val AccessControlAllowOrigin      = "Access-Control-Allow-Origin"
  final val AccessControlExposeHeaders    = "Access-Control-Expose-Headers"
  final val AccessControlMaxAge           = "Access-Control-Max-Age"
  final val AccessControlRequestHeaders   = "Access-Control-Request-Headers"
  final val AccessControlRequestMethod    = "Access-Control-Request-Method"
  final val Age                           = "Age"
  final val Allow                         = "Allow"
  final val Authorization                 = "Authorization"
  final val CacheControl                  = "Cache-Control"
  final val Connection                    = "Connection"
  final val ContentBase                   = "Content-Base"
  final val ContentDisposition            = "Content-Disposition"
  final val ContentEncoding               = "Content-Encoding"
  final val ContentLanguage               = "Content-Language"
  final val ContentLength                 = "Content-Length"
  final val ContentLocation               = "Content-Location"
  final val ContentTransferEncoding       = "Content-Transfer-Encoding"
  final val ContentMd5                    = "Content-Md5"
  final val ContentRange                  = "Content-Range"
  final val ContentType                   = "Content-Type"
  final val Cookie                        = "Cookie"
  final val Date                          = "Date"
  final val Etag                          = "Etag"
  final val Expect                        = "Expect"
  final val Expires                       = "Expires"
  final val From                          = "From"
  final val Host                          = "Host"
  final val IfMatch                       = "If-Match"
  final val IfModifiedSince               = "If-Modified-Since"
  final val IfNoneMatch                   = "If-None-Match"
  final val IfRange                       = "If-Range"
  final val IfUnmodifiedSince             = "If-Unmodified-Since"
  final val LastModified                  = "Last-Modified"
  final val Location                      = "Location"
  final val MaxForwards                   = "Max-Forwards"
  final val Origin                        = "Origin"
  final val Pragma                        = "Pragma"
  final val ProxyAuthenticate             = "Proxy-Authenticate"
  final val ProxyAuthorization            = "Proxy-Authorization"
  final val Range                         = "Range"
  final val Referer                       = "Referer"
  final val RetryAfter                    = "Retry-After"
  final val Server                        = "Server"
  final val SetCookie                     = "Set-Cookie"
  final val SetCookie2                    = "Set-Cookie2"
  final val Te                            = "Te"
  final val Trailer                       = "Trailer"
  final val TransferEncoding              = "Transfer-Encoding"
  final val Upgrade                       = "Upgrade"
  final val UserAgent                     = "User-Agent"
  final val Vary                          = "Vary"
  final val Via                           = "Via"
  final val Warning                       = "Warning"
  final val WwwAuthenticate               = "Www-Authenticate"
  final val xForwardedFor                 = "X-Forwarded-For"
  final val xForwardedProto               = "X-Forwarded-Proto"
}

import HttpHeader._

trait HttpHeaderAccess { this: HttpHeader =>
  def get(key: String): Option[String]
  def getAll(key: String): Seq[String]
  def isEmpty: Boolean
  def toSeq: Seq[HeaderEntry]
}

/**
  * An immutable multi-map implementation for storing HTTP headers
  */
case class HttpHeader(private val map: Map[String, Any]) extends HttpHeaderAccess {
  override def toString: String = {
    toSeq.mkString(", ")
  }

  def set(key: String, value: String): HttpHeader = {
    this.copy(
      map = map + (key -> value)
    )
  }
  def add(key: String, value: String): HttpHeader = {
    val newMap = map
      .get(key).map { v =>
        val newValue = v match {
          case s: String                   => Seq(s, value)
          case lst: Seq[String @unchecked] => lst +: value
        }
        map + (key -> newValue)
      }.getOrElse {
        map + (key -> value)
      }

    this.copy(map = newMap)
  }
  def remove(key: String): HttpHeader = {
    this.copy(map = map - key)
  }

  override def get(key: String): Option[String] = {
    map.get(key).flatMap { v: Any =>
      v match {
        case s: String                   => Some(s)
        case lst: Seq[String @unchecked] => Option(lst.head)
        case null                        => None
        case _                           => Some(v.toString)
      }
    }
  }

  override def getAll(key: String): Seq[String] = {
    map
      .get(key).map { v: Any =>
        v match {
          case s: String                   => Seq(s)
          case lst: Seq[String @unchecked] => lst
          case null                        => Seq.empty
          case v                           => Seq(v.toString)
        }
      }.getOrElse(Seq.empty)
  }

  override def isEmpty: Boolean = map.isEmpty

  override def toSeq: Seq[HeaderEntry] = {
    val b = Seq.newBuilder[HeaderEntry]
    for ((k, v) <- map) {
      v match {
        case s: String =>
          b += HeaderEntry(k, s)
        case lst: Seq[String @unchecked] =>
          b ++= lst.map { x => HeaderEntry(k, x) }
        case null =>
        // do nothing
        case other =>
          b += HeaderEntry(k, other.toString)
      }
    }
    b.result()
  }
}
