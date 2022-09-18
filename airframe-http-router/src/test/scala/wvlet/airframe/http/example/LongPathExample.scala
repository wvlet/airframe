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
package wvlet.airframe.http.example

import wvlet.airframe.http.{Endpoint, HttpMethod}

case class PathEntry(scope: String, key: String)

/**
  * ..
  */
trait LongPathExample {
  // Adding this entry to check *key match in /v1/config/entry/:scope/*key if the key contains `clusters` tokien
  @Endpoint(path = "/v1/clusters")
  def getClusters = {}

  @Endpoint(path = "/v1/config/entry", method = HttpMethod.GET)
  def getAll(): Unit = {}

  @Endpoint(path = "/v1/config/entry/:scope", method = HttpMethod.GET)
  def getAllInScope(scope: String): Unit = {}

  @Endpoint(path = "/v1/config/entry/:scope/*key", method = HttpMethod.GET)
  def get(scope: String, key: String): PathEntry = {
    PathEntry(scope, key)
  }
}
