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
  * If you need to terminate a session explicitly (e.g., via REST call), bind[Session] is useful.
  */
object DI_20_BindSession extends App {
  import wvlet.airframe._

  trait MyApi extends LogSupport {
    private val session = bind[Session]

    def shutdown = {
      warn("shutdown is called")
      session.shutdown
    }
  }

  newDesign.build[MyApi] { api => api.shutdown }
}
