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
  * If you have services that have some resources (e.g., database connections, http clients, thread managers, etc.),
  * define trait XXXService and add lifecycle hooks to the trait to properly release such resources.
  *
  * Service traits defined in this manner will be reusable components that can safely manage your resources upon session
  * termination.
  *
  * The lifecycle of resources will be managed in First-In Last-Out (FILO) order. Even if you use these service traits
  * multiple times, shutdown hooks will be called only once per the resource.
  */
object DI_12_ReusingService extends App {
  import wvlet.airframe.*

  class DB extends LogSupport with AutoCloseable {
    def query(sql: String) = {}
    def connect: Unit = {
      info("connected")
    }
    override def close(): Unit = {
      info("closed")
    }
  }
  class HttpClient extends LogSupport with AutoCloseable {
    def send(request: String) = {}
    def connect: Unit = {
      info("connected")
    }
    override def close(): Unit = {
      info("closed")
    }
  }

  class C1(db: DB, httpClient: HttpClient) {
    db.query("select 1")
    httpClient.send("GET /")
  }

  class C2(httpClient: HttpClient) {
    // Sharing the same http client instance with C1
    httpClient.send("POST /data")
  }

  class MyApp(
      c1: C1, // Uses db and httpClient
      c2: C2  // uses httpClient
  )

  val d = newSilentDesign
  d.build[MyApp] { app =>
    //
  }
  // db, httpClient will be closed only once
}
