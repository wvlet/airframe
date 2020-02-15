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
package wvlet.airframe.examples.di

import wvlet.log.LogSupport

/**
  * To reuse same type data for different purposes, use type aliases.
  * Airframe treats type aliases with different names as different types when binding dependencies.
  */
object DI_10_TypeAliasBinding extends App {
  import wvlet.airframe._

  case class DbConfig(db: String)

  // Define type aliases of DbConfig
  type LogDbConfig  = DbConfig
  type ChatDbConfig = DbConfig

  trait MyApp extends LogSupport {
    val logDbConfig  = bind[LogDbConfig]
    val chatDbConfig = bind[ChatDbConfig]

    def run: Unit = {
      info(s"logdb: ${logDbConfig}, chatdb: ${chatDbConfig}")
    }
  }

  val d = newSilentDesign
    .bind[LogDbConfig].toInstance(DbConfig("log"))
    .bind[ChatDbConfig].toInstance(DbConfig("chat"))

  d.build[MyApp] { app => app.run }
}
