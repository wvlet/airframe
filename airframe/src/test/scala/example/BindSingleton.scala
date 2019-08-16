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
package example

import javax.annotation.{PostConstruct, PreDestroy}
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

/**
  * bindSingleton[X] example
  */
object BindSingleton {
  import wvlet.airframe._

  trait X {
    def hello: String   = "Hello"
    def goodbye: String = "Good-bye"
  }
  trait Y {
    def world: String = "World"
  }

  trait XY extends LogSupport {
    val x = bind[X]
    val y = bind[Y]

    @PostConstruct
    def start(): Unit = {
      // This will be called only once since XY is used as singleton
      debug(s"${x.hello} ${y.world}!")
    }

    @PreDestroy
    def close(): Unit = {
      // This will be called only once since XY is used as singleton
      debug(s"${x.goodbye} ${y.world}!")
    }
  }

  // Use bind[XY] for sharing a singleton of XY between App1 and App2
  trait XYService {
    val service = bind[XY]
  }

  trait App1 extends XYService
  trait App2 extends XYService

  val session = newSilentDesign.newSession
  val app     = session.build[App1] // shows "Hello World!"
  val app2    = session.build[App2] // shows nothing since XY is already initialized
  session.shutdown // shows "Good-bye World!"
}

class BindSingleton extends AirSpec {
  scalaJsSupport

  def `run`: Unit = {
    val b = BindSingleton
  }
}
