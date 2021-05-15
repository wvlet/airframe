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
package wvlet.airframe.config

import wvlet.airspec.AirSpec

/**
  */
class NestedConfigTest extends AirSpec {
  import NestedConfigTest._
  test("support nested case classes") {
    val configPaths = Seq("airframe-config/src/test/resources")

    val config = Config(env = "default", configPaths = configPaths)
      .registerFromYaml[ServerPoolConfig]("nested.yml")

    val s = config.toString
    debug(s)
  }
}

object NestedConfigTest {
  case class ServerConfig(host: String, port: Int)
  case class ServerPoolConfig(servers: IndexedSeq[ServerConfig])
}
