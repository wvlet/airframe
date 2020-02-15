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
    // server port. -1 if hte port is unknown
    port: Int,
    // http or https
    scheme: Option[String] = None
) {
  override def toString: String = hostAndPort

  // Returns host:port string without the protcol scheme like http://, https://
  def hostAndPort: String = s"${host}:${port}"

  // Returns URI with the protocol schema (if specified)
  def uri: String = {
    val prefix = scheme
      .map { x => s"${x}://${host}" }.getOrElse(host)
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
      val uri = URI.create(address)
      val port = if (uri.getPort != -1) {
        uri.getPort
      } else {
        uri.getScheme match {
          case "https" => 443
          case "http"  => 80
          case _       => -1
        }
      }
      ServerAddress(uri.getHost, port, Some(uri.getScheme))
    } else {
      val pos = address.indexOf(":")
      if (pos > 0) {
        val port = address.substring(pos + 1, address.length).toInt
        val scheme = port match {
          case 80  => Some("http")
          case 443 => Some("https")
          case _   => None
        }
        ServerAddress(address.substring(0, pos), port, scheme)
      } else {
        ServerAddress(address, -1, None)
      }
    }
  }
}
