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
  * bindFactory[X => Y] example
  */
object BindFactory {
  import wvlet.airframe._

  trait X {
    def hello: String   = "Hello"
    def goodbye: String = "Good-bye"
  }
  trait Y {
    def world: String = "World"
  }

  class XY(x: X, y: Y) extends LogSupport {
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

  trait Z extends LogSupport {
    def world = bind[String]

    @PostConstruct
    def start(): Unit = {
      // This will be called only once since XY is used as singleton
      debug(s"Hello ${world}!")
    }

    @PreDestroy
    def close(): Unit = {
      // This will be called only once since XY is used as singleton
      debug(s"Good-bye ${world}!")
    }
  }

  trait XYFactoryService {
    val factory1 = bindFactory[X => XY]
    val factory2 = bindFactory[String => Z]
  }

  trait App1 extends XYFactoryService {
    factory1(new X {
      override def hello   = "Hi"
      override def goodbye = "Bye"
    })
  }

  trait App2 extends XYFactoryService {
    factory2("Scala")
  }

  val session = newSilentDesign
    .bind[Y].toSingleton
    .newSession
  val app1 = session.build[App1] // shows "Hi World!"
  val app2 = session.build[App2] // shows "Hello Scala!"
  session.shutdown // shows "Bye World!" and "Good-bye Scala!"
}

class BindFactory extends AirSpec {
  scalaJsSupport
  def `run`: Unit = {
    val b = BindFactory
  }
}
