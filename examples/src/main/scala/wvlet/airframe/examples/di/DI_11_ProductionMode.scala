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

import javax.annotation.PostConstruct
import wvlet.log.LogSupport

/**
  */
object DI_11_ProductionMode extends App with LogSupport {
  import wvlet.airframe._

  trait MyService extends LogSupport {
    @PostConstruct
    def init: Unit = {
      info("initialized")
    }
  }

  trait MyApp {}

  val d = newSilentDesign
    .bind[MyService].toSingleton // To eagerly initialize the service, you must bind it to the design

  info("Production mode")
  d.withProductionMode.build[MyApp] { app =>
    // MyService is already initialized
  }

  info("Lazy mode")
  d.build[MyApp] { app =>
    // MyService will not be initialized here
  }
}
