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

import javax.annotation.{PostConstruct, PreDestroy}
import wvlet.log.LogSupport

/**
  * To add lifecycle hooks, JSR250 annotations (@PostConstruct, @PreDestroy) can be used too.
  *
  * This is useful if you need to define lifecycle management hooks as methods in your trait.
  */
object DI_07_JSR250Annotation extends App {
  class MyService extends LogSupport {
    @PostConstruct
    def start = {
      info("starting the service")
    }

    @PreDestroy
    def stop = {
      info("stopping the service")
    }
  }

  import wvlet.airframe.*

  newSilentDesign.build[MyService] { service =>
    // PostConstrct method will be called here
  }
  // PreDestroy method will be called
}
