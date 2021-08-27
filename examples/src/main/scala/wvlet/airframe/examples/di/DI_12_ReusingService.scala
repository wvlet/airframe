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
  import wvlet.airframe._

  trait DB extends LogSupport {
    def query(sql: String) = {}
    def connect: Unit = {
      info("connected")
    }
    def close(): Unit = {
      info("closed")
    }
  }
  trait HttpClient extends LogSupport {
    def send(request: String) = {}
    def connect: Unit = {
      info("connected")
    }
    def close(): Unit = {
      info("closed")
    }
  }

  trait DBService {
    val db = bind[DB]
      .onStart(_.connect)
      .onShutdown(_.close())
  }

  trait HttpClientService {
    // Binding will inject a singleton by default
    val httpClient = bind[HttpClient]
      .onStart(_.connect)
      .onShutdown(_.close())
  }

  trait C1 extends DBService with HttpClientService {
    db.query("select 1")
    httpClient.send("GET /")
  }

  trait C2 extends HttpClientService {
    // Sharing the same http client instance with C1
    httpClient.send("POST /data")
  }

  trait MyApp {
    val c1 = bind[C1] // Uses db and httpClient
    val c2 = bind[C2] // uses httpClient
  }

  val d = newSilentDesign
  d.build[MyApp] { app =>
    //
  }
  // db, httpClient will be closed only once
}
