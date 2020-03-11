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
import java.net.URI

import wvlet.log.LogSupport

/**
  * Server address holder
  */
case class ServerAddress(
    host: String,
    // server port. -1 if http port is unknown
    port: Int,
    // http or https
    scheme: String = "http"
) {
  override def toString: String = hostAndPort

  // Returns host:port string without the protcol scheme like http://, https://
  def hostAndPort: String = s"${host}:${port}"

  // Returns URI with the protocol scheme (if specified)
  def uri: String = {
    val prefix = s"${scheme}://${host}"
    if (port != -1) {
      s"${prefix}:${port}"
    } else {
      prefix
    }
  }
}

object ServerAddress extends LogSupport {
  def apply(address: String): ServerAddress = {
    if (address.matches("""\w+:\/\/.*""")) {
      val uri         = URI.create(address)
      val givenScheme = Option(uri.getScheme)
      val (port, scheme) = uri.getPort match {
        case 443 =>
          (443, givenScheme.getOrElse("https"))
        case -1 =>
          // When the port is unspecified, guess the port from the uri scheme
          givenScheme match {
            case Some("https") =>
              (443, "https")
            case other =>
              (80, givenScheme.getOrElse("http"))
          }
        case other =>
          (other, givenScheme.getOrElse("http"))
      }
      ServerAddress(uri.getHost, port, scheme)
    } else {
      val pos = address.indexOf(":")
      if (pos > 0) {
        val port = address.substring(pos + 1, address.length).toInt
        val scheme = port match {
          case 443 => "https"
          case _   => "http"
        }
        ServerAddress(address.substring(0, pos), port, scheme)
      } else {
        ServerAddress(address, 80, "http")
      }
    }
  }
}
