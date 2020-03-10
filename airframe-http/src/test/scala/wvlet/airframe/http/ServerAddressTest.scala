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

import wvlet.airspec.AirSpec

/**
  *
  */
class ServerAddressTest extends AirSpec {
  val examples = Seq(
    (ServerAddress("localhost:12345"), ServerAddress("localhost", 12345), "localhost:12345", "localhost:12345"),
    (
      ServerAddress("wvlet.org:80"),
      ServerAddress("wvlet.org", 80, Some("http")),
      "http://wvlet.org:80",
      "wvlet.org:80"
    ),
    (
      ServerAddress("wvlet.org:443"),
      ServerAddress("wvlet.org", 443, Some("https")),
      "https://wvlet.org:443",
      "wvlet.org:443"
    ),
    (
      ServerAddress("http://localhost"),
      ServerAddress("localhost", 80, Some("http")),
      "http://localhost:80",
      "localhost:80"
    ),
    (
      ServerAddress("https://localhost"),
      ServerAddress("localhost", 443, Some("https")),
      "https://localhost:443",
      "localhost:443"
    ),
    (
      ServerAddress("https://localhost:444"),
      ServerAddress("localhost", 444, Some("https")),
      "https://localhost:444",
      "localhost:444"
    ),
    (
      ServerAddress("http://wvlet.org:8080"),
      ServerAddress("wvlet.org", 8080, Some("http")),
      "http://wvlet.org:8080",
      "wvlet.org:8080"
    )
  )

  test("parse host and port") {
    for (x <- examples) {
      x._1 shouldBe x._2
      x._1.uri shouldBe x._3
      x._1.hostAndPort shouldBe x._4
      x._1.toString shouldBe x._4
    }
  }
}
