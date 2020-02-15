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
  * To switch the implementation, override the binding in the design
  */
object DI_08_OverrideBinding extends App {
  import wvlet.airframe._

  trait DB extends LogSupport {
    def query(sql: String): Unit = {
      info(s"Execute: ${sql}")
    }
  }

  trait MockDB extends DB {
    override def query(sql: String): Unit = {
      info(s"Dryrun: ${sql}")
    }
  }

  trait MyApp extends LogSupport {
    private val db = bind[DB]

    def run: Unit = {
      db.query("select 1")
    }
  }

  val coreDesign = newSilentDesign

  coreDesign.build[MyApp] { app => app.run }

  // Switch the implementation of DB to MockDB
  val testDesign = coreDesign
    .bind[DB].to[MockDB]

  testDesign.build[MyApp] { app => app.run }
}
